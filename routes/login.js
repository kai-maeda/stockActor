module.exports = function(db) {    
    const express = require('express');
    const router = express.Router();

    router.all('/:interfaceType-interface/login', (req, res) => {
        const interfaceType = req.params.interfaceType;
        res.render('login', { interfaceType: interfaceType });
    });
    router.all('/:interfaceType-interface/create-account', (req, res) => {
        const interfaceType = req.params.interfaceType;
        res.render('create-account', { interfaceType: interfaceType });
    });
    router.all('/:interfaceType-interface/register', async (req, res) => {
        const interfaceType = req.params.interfaceType;
        const { state_id, tax_id, cname, phone, email, username, password } = req.body;

        // Check if tax_id is an integer
        if (!Number.isInteger(parseInt(tax_id))) {
            return res.status(400).send('Tax ID must be an integer.');
        }

        // Check if username already exists in the database
        try {
            const [rows] = await db.query(`SELECT COUNT(*) AS count FROM ${interfaceType} WHERE username = ?`, [username]);
            if (rows[0].count > 0) {
                return res.status(400).send('Username already exists. Please choose a different username.');
            }
        } catch (error) {
            console.error('Error checking username:', error);
            return res.status(500).send('Error processing request.');
        }
        const insertQuery = 
            `INSERT INTO ${interfaceType} (state_id, tax_id, cname, phone, email, username, password)
            VALUES (?, ?, ?, ?, ?, ?, ?)`;
        try {
            await db.query(insertQuery, [state_id, tax_id, cname, phone, email, username, password]);
            console.log('${interfaceType} added successfully');
            res.render('login', { interfaceType: interfaceType });
        } catch (error) {
            console.error('Error adding ${interfaceType}:', error);
            res.status(500).send('Error creating ${interfaceType} account.');
        }
    });

    router.all('/:interfaceType-interface/validate-login', async (req, res) => {
        const { username, password } = req.body;
        const interfaceType = req.params.interfaceType; // 'customer' or 'manager'
        const query = `SELECT * FROM ${interfaceType} WHERE username = ? AND password = ? LIMIT 1`;
        const query2 = 'SELECT acc_id FROM Account_Has WHERE username = ?';
        const query3 = 'SELECT balance FROM Market_Account WHERE username = ? AND acc_id = ?';
        const query4 = 'SELECT * FROM Stock_Account WHERE username = ? AND acc_id = ?';
        // const query5 = 'SELECT SUM(buy_price * shares_bought) AS total_cost FROM Bought_Stock WHERE acc_id = ? AND symbol = ?';
        let acc_id;
        let balance;
        let position = 0;
        try {
            const [rows] = await db.query(query, [username,password]);
            if (rows.length === 0) {
                return res.status(401).send('Invalid username or password.');
            }
            const[rows2] = await db.query(query2, [username]);
            console.log(rows2.length);
            if (rows2.length == 0) {
                acc_id = 0;
                console.log("about to enter while loop");
                while (true) {
                    acc_id = Math.floor(Math.random() * Math.pow(2, 31));
                    const [find_acc_id] = await db.query('SELECT * FROM Account_Has WHERE username = ? AND acc_id = ?', [username, acc_id]);
                    if(find_acc_id.length == 0) {
                        break;
                    }
                }
                console.log("about to insert fresh customer market");
                await db.query('INSERT INTO Account_Has VALUES (?,?)', [acc_id, username]);
                await db.query('INSERT INTO Market_Account VALUES (?,?,?)', [0, acc_id, username]);
            }
            else {
                acc_id = rows2[0].acc_id;
                console.log("already inserted");
            
            }
            console.log("done with row1 and row2");
            const[rows3] = await db.query(query3, [username,acc_id]);
            balance = rows3[0].balance;
            const[rows4] = await db.query(query4, [username,acc_id]); //stock_account might be empty
            for (let i = 0; i < rows4.length; i++) {
                const stock = rows4[i];
                const [actorDetails] = await db.query('SELECT * FROM Stock_Actor WHERE symbol = ?', [stock.symbol]);
                // const [cost] = (await db.query(query5, [acc_id,stock.symbol]));
                Object.assign(stock, actorDetails[0]);
                // Object.assign(stock, cost[0]);
                position += parseFloat(stock.balance_share);
            }
            balance = parseFloat(balance).toFixed(2);
            position = parseFloat(position).toFixed(2);
            const transactions = await getTransactionHistory(username, acc_id);
            console.log("about to render");
            res.render('traderInterface', {menu: 'dashboard', username, acc_id, balance,position, stockData: rows4, transactions});
        } catch (error) { console.log(error);}
    });

    async function getTransactionHistory(username, acc_id) {
        const transactions = [];
        try {
            const [result] = await db.query('SELECT * FROM Commits WHERE username = ? AND acc_id = ?', [username, acc_id]);
            // console.log(result);
            for (let i = 0; i < result.length; i++) {
                const row = result[i];
                // console.log("Current TID:", row.tid);  // This should log the tid
                const tResult = await db.query('SELECT * FROM Transactions WHERE tid = ?', [row.tid]);
                // console.log("Transaction Result for TID", row.tid, ":", tResult);
                // console.log(tResult);
                if (tResult.length > 0) {
                    // console.log(tResult[0][0].date_executed);
                    let transaction = {
                        id: row.tid,
                        dateExecuted: tResult[0][0].date_executed ? tResult[0][0].date_executed.toISOString().slice(0, 10) : 'Unknown Date',
                        // Initialize placeholders for additional details
                        type: null,
                        details: {},
                    };
                    // console.log(transaction);
                    const buyResult = await db.query('SELECT * FROM Buy WHERE tid = ?', [row.tid]);
                    // console.log(buyResult[0][0]);
                    if (typeof buyResult[0][0] !== 'undefined') {
                        transaction.type = 'Buy';
                        transaction.details = buyResult[0][0]; // Assuming each transaction ID will only match one row per table
                        transactions.push(transaction);
                        continue; // Skip to the next transaction
                    }
        
                    const sellResult = await db.query('SELECT * FROM Sell WHERE tid = ?', [row.tid]);
                    if (typeof sellResult[0][0] !== 'undefined') {
                        transaction.type = 'Sell';
                        transaction.details = sellResult[0][0];
                        transactions.push(transaction);
                        continue;
                    }
        
                    const depositResult = await db.query('SELECT * FROM Deposit WHERE tid = ?', [row.tid]);
                    if (typeof depositResult[0][0] !== 'undefined') {
                        transaction.type = 'Deposit';
                        transaction.details = depositResult[0][0];
                        transactions.push(transaction);
                        continue;
                    }
        
                    const withdrawResult = await db.query('SELECT * FROM Withdraw WHERE tid = ?', [row.tid]);
                    if (typeof withdrawResult[0][0] !== 'undefined') {
                        transaction.type = 'Withdraw';
                        transaction.details = withdrawResult[0][0];
                        transactions.push(transaction);
                        continue;
                    }
                }
            }
            return transactions;
        } catch (error) {
            console.error('Error fetching transaction history:', error);
        }
    }

    

    return router;
}