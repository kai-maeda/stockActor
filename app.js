const express = require('express');
const mysql = require('mysql2');
const path = require('path');
const bodyParser = require('body-parser');

const app = express();
app.use(express.static('public'));
app.use('/images', express.static('images'));
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.use(bodyParser.urlencoded({ extended: true }));

// Serve your index.html file
app.get('/', (req, res) => {
    res.sendFile(__dirname + '/index.html');
});

// MySQL connection
const db = mysql.createConnection({
    host: process.env.MYSQL_ADDON_HOST,       // Environment variable for the DB host
    user: process.env.MYSQL_ADDON_USER,       // Environment variable for the DB user
    password: process.env.MYSQL_ADDON_PASSWORD, // Environment variable for the DB password
    database: process.env.MYSQL_ADDON_DB     // Environment variable for the DB name
}).promise();

const indexRouter = require('./routes/index')(db);
const loginRouter = require('./routes/login')(db);
const demoRouter = require('./routes/demo')(db);
const traderRouter = require('./routes/trader')(db, createTransaction, getDateSQLFriendly);

app.use(indexRouter);
app.use(loginRouter);
app.use(demoRouter);
app.use(traderRouter);
app.use(express.json());

app.post('/validate-symbol', async (req, res) => {
    const { symbol } = req.body;
    try {
        let symbolExists = false;
        const [rows] = await db.query('SELECT * FROM stock_actor WHERE symbol = ?', [symbol]);
        if(rows.length > 0) {
            symbolExists = true;
        }
        console.log(symbolExists, symbol);
        if (symbolExists) {
            res.sendStatus(200);
        } else {
            res.sendStatus(404);
        }
    } catch (error) {}
});

app.post('/change-date', async (req, res) => {
    const { newDate } = req.body; // Make sure this matches the JSON key sent from the client
    if (!newDate) {
        return res.status(400).json({ error: "New date is required." });
    }
    try {
        await db.query('UPDATE curr_time SET curr_date = ?', [newDate]);
        res.sendStatus(200);
        res.json({ success: true, message: "Date updated successfully." });
    } catch (error) {}
});

app.post('/trade', async (req, res) => {
    const { username, symbol, acc_id } = req.body;
    let num_share = 0
    try {
        // console.log("in /trade");
        const accountDetails = (await db.query('SELECT * FROM stock_account WHERE symbol = ? AND username = ? AND acc_id = ?', [symbol, username, acc_id]))[0];
        const stockDetails = (await db.query('SELECT * FROM stock_actor WHERE symbol = ?', [symbol]))[0];
        console.log(stockDetails[0]);
        if(typeof stockDetails[0] == 'undefined') {
            return res.json({ error: 'A stock with this symbol does not exist.' });
        }
        if(accountDetails.length > 0) {
            num_share = accountDetails[0].num_share;
        }
        // console.log(num_share);
        res.json({
            symbol: symbol,
            current_price: stockDetails[0].current_price,
            closing_price: stockDetails[0].closing_price,
            num_share: num_share
        });
    } catch (error) {}
});
app.all('/trade-update', async (req,res) => {
    let { username, acc_id, balance, position, stockData, symbol, action, quantity } = req.body;
    const cash = balance - position;
    let accountDetails;
    let stockDetails;
    let [rows4] = [];
    try {
        accountDetails = (await db.query('SELECT * FROM stock_account WHERE symbol = ? AND username = ? AND acc_id = ?', [symbol, username, acc_id]))[0][0];
        stockDetails = (await db.query('SELECT * FROM stock_actor WHERE symbol = ?', [symbol]))[0][0];
        const totalValue = stockDetails.current_price * quantity;
        
        if (action === 'buy') { //add to bought_stock
            // console.log("buying stock...");
            if (cash < totalValue) {
                return res.json({ error: 'You do not have enough cash to make this purchase.' });
            }
            // console.log(accountDetails);
            if (typeof accountDetails == 'undefined') {
                await db.query('INSERT INTO Stock_Account VALUES (?,?,?,?,?);', [acc_id, symbol ,quantity, totalValue, username]);
                accountDetails = (await db.query('SELECT * FROM stock_account WHERE symbol = ? AND username = ? AND acc_id = ?', [symbol, username, acc_id]))[0][0];
                [rows4] = await db.query('SELECT * FROM Stock_Account WHERE username = ? AND acc_id = ?', [username,acc_id]); //stock_account might be empty
                console.log(rows4);
                for (let i = 0; i < rows4.length; i++) {
                    const stock = rows4[i];
                    const [actorDetails] = await db.query('SELECT * FROM stock_actor WHERE symbol = ?', [stock.symbol]);
                    // const [cost] = (await db.query(query5, [acc_id,stock.symbol]));
                    Object.assign(stock, actorDetails[0]);
                    // Object.assign(stock, cost[0]);
                }
            } else{
                console.log(accountDetails.balance_share, totalValue, accountDetails.num_share, quantity);
                const new_balance = (parseFloat(accountDetails.balance_share) + parseFloat(totalValue));
                await db.query('UPDATE stock_account SET num_share = num_share + ?, balance_share = ? WHERE acc_id = ? AND symbol = ? AND username = ?', [quantity, new_balance, acc_id, symbol,username]);
                [rows4] = await db.query('SELECT * FROM Stock_Account WHERE username = ? AND acc_id = ?', [username,acc_id]); //stock_account might be empty
                console.log(rows4);
                for (let i = 0; i < rows4.length; i++) {
                    const stock = rows4[i];
                    const [actorDetails] = await db.query('SELECT * FROM stock_actor WHERE symbol = ?', [stock.symbol]);
                    // const [cost] = (await db.query(query5, [acc_id,stock.symbol]));
                    Object.assign(stock, actorDetails[0]);
                    // Object.assign(stock, cost[0]);
                }
            } 
            // console.log("updated stock account");
            const tid = await createTransaction(username,acc_id);
            // console.log("created transaction");
            await db.query('INSERT INTO Buy (tid, symbol, shares, buy_price) VALUES (?, ?, ?, ?)', [tid, symbol, quantity, stockDetails.current_price]);
            position = parseFloat(position) + parseFloat(totalValue);
            accountDetails.num_share = parseInt(accountDetails.num_share) + parseInt(quantity);
        } else if (action === 'sell') {
            if (typeof accountDetails == 'undefined' || accountDetails.num_share < quantity) {
                return res.json({ error: 'You cannot sell more stock than you own.' });
            }
            const new_balance = (parseFloat(accountDetails.balance_share) - parseFloat(accountDetails.balance_share) / parseInt(accountDetails.num_share) * parseInt(quantity));
            await db.query('UPDATE stock_account SET num_share = num_share - ?, balance_share = ? WHERE acc_id = ? AND symbol = ? AND username = ?', [quantity, new_balance, acc_id, symbol,username]);
            [rows4] = await db.query('SELECT * FROM Stock_Account WHERE username = ? AND acc_id = ?', [username,acc_id]); //stock_account might be empty
            console.log(rows4);
            for (let i = 0; i < rows4.length; i++) {
                const stock = rows4[i];
                const [actorDetails] = await db.query('SELECT * FROM stock_actor WHERE symbol = ?', [stock.symbol]);
                // const [cost] = (await db.query(query5, [acc_id,stock.symbol]));
                Object.assign(stock, actorDetails[0]);
                // Object.assign(stock, cost[0]);
            }
            const tid = await createTransaction(username,acc_id);
            await db.query('INSERT INTO Sell (tid, symbol, sell_price, shares) VALUES (?, ?, ?, ?)', [tid, symbol,stockDetails.current_price, quantity]);
            position = parseFloat(position) - parseFloat(totalValue);
            accountDetails.num_share = parseInt(accountDetails.num_share) - parseInt(quantity);
        } else {return res.status(400).json({ error: "Invalid action" });}
        console.log("curr and quant", stockDetails.current_price);
        console.log("curr and quant",quantity);
        console.log("position in trade-update", position, totalValue);
    } catch (error) {}
    // console.log(balance);
    res.json({
        symbol: symbol,
        current_price: stockDetails.current_price,
        closing_price: stockDetails.closing_price,
        num_share: accountDetails.num_share, // This should be updated based on your buy/sell logic
        position: position,
        balance,
        stockData: rows4
    });
});


async function createTransaction(username, acc_id) {
    let transaction_id = 0;
    while (true) {
        transaction_id = Math.floor(Math.random() * Math.pow(2, 31));
        const [rows] = await db.query('SELECT * FROM Transactions WHERE tid = ?', [transaction_id]);
        if(rows.length == 0) {
            break;
        }
    }
    console.log("tid", transaction_id);
    const date = await getDateSQLFriendly();
    await db.query('INSERT INTO Transactions (tid, date_executed) VALUES (?, ?)', [transaction_id,date]);
    await db.query('INSERT INTO Commits (acc_id, tid, username) VALUES (?, ?, ?)', [acc_id, transaction_id, username]);
    await db.query('UPDATE Customer SET last_tid = ? WHERE username = ?',[transaction_id, username]);

    return (transaction_id);
}
async function getDateSQLFriendly() {
    const current_time =(await db.query('SELECT * FROM Curr_Time'))[0][0];
    const curr_date = current_time.curr_date; 

    const customDate = new Date(curr_date);
    const formattedDate = customDate.toISOString().split('T')[0];

    return formattedDate;
}

const port = process.env.PORT || 3000;
app.listen(port, () => {
    console.log(`Server running on port ${port}`);
});
