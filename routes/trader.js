module.exports = function(db,createTransaction,getDateSQLFriendly) {    
    const express = require('express');
    const router = express.Router();

    router.all('/trader', async (req, res) => {
        const menu = req.query.menu || 'dashboard'; // Get the value of menu from the query parameter or set it to 'dashboard' by default
        const username = req.query.username;
        const acc_id = req.query.acc_id;
        const balance = req.query.balance;
        const position = req.query.position;
        let stockData = req.query.stockData;
        try {
            if (typeof stockData === 'string') {
                stockData = JSON.parse(stockData);
            }
        } catch (error) {}
        if (menu === 'transfer') {
            res.render('traderInterface', { menu: 'transfer', username, acc_id, balance, position, stockData });
        } else if(menu == 'dashboard') {
            const transactions = await getTransactionHistory(username, acc_id);
            res.render('traderInterface', { menu: 'dashboard', username, acc_id, balance, position, stockData, transactions });
        } else if(menu == 'trade') {
            res.render('traderInterface', { menu: 'trade', username, acc_id, balance, position, stockData });
        } else {
            try {
                const[rows] = await db.query('SELECT * FROM Movie'); //stock_account might be empty
                const[rows2] = await db.query('SELECT * FROM Stock_Actor'); //stock_account might be empty
                console.log("queries worked.");
                res.render('traderInterface', { menu: 'research', username, acc_id, balance, position, stockData, movies: rows, stocks: rows2,stockInfo: false,  movieInfo: false});
            } catch(error) {console.log(error);}
        }
    });
    router.all('/transfer', async (req, res) => {
        const username = req.body.username;
        const acc_id = req.body.acc_id;
        let balance = parseFloat(req.body.balance);
        const position = req.body.position;
        let stockData = req.body.stockData;
        try {
            if (typeof stockData === 'string') {
                stockData = JSON.parse(stockData);
            }
        } catch (error) {}

        const fromAccount = req.body.fromAccount;
        const toAccount = req.body.toAccount;
        const transferAmount = parseFloat(req.body.transferAmount);

        console.log(fromAccount, toAccount, transferAmount);

        if (isNaN(transferAmount) || transferAmount <= 0) {
            console.log(balance, position);
            res.render('traderInterface', { menu: 'transfer', username, acc_id, balance, position, stockData, error: 'Invalid transfer amount. Please enter a valid number greater than zero.'}); 
        }
        if (fromAccount == "Bank" && toAccount == `${username} - ${acc_id}`) {
            await db.query(`UPDATE Market_Account SET balance = balance + ? WHERE username = ?`, [transferAmount, username]);  
            const tid = await createTransaction(username,acc_id);
            await db.query('INSERT INTO Deposit (tid,amount) VALUES (?, ?)', [tid, transferAmount]);
            balance += transferAmount; 
        } else if (fromAccount == `${username} - ${acc_id}` && toAccount == "Bank") {
            if(transferAmount > balance) {
                res.render('traderInterface', { menu: 'transfer', username, acc_id, balance, position, stockData, error: 'You cannot extract more money from your account than you have.'}); 
            }
            await db.query(`UPDATE Market_Account SET balance = balance - ? WHERE username = ?`, [transferAmount, username]);
            const tid = await createTransaction(username,acc_id);
            await db.query('INSERT INTO Withdraw (tid,amount) VALUES (?, ?)', [tid, transferAmount]);
            balance -= transferAmount; 
        } else {
            console.log(balance, position);
            res.render('traderInterface', { menu: 'transfer', username, acc_id, balance, position, stockData, error: 'Source and Destination of transfer cannot be the same.'}); 
        }
        res.render('traderInterface', { menu: 'transfer', username, acc_id, balance, position, stockData }); 
    });

    router.all('/trade-pos', async (req,res) => {
        let { username, acc_id, balance, position, stockData } = req.body;
        try {
            if (typeof stockData === 'string') {
                stockData = JSON.parse(stockData);
            }
        } catch (error) {}
        // console.log("pos",position);
        // console.log(stockData);
        res.render('traderInterface', { menu: 'trade', username, acc_id, balance, position, stockData}); 
        // res.json({});
    });
    async function getTransactionHistory(username, acc_id) {
        const transactions = [];
        try {
            const [result] = await db.query('SELECT * FROM Commits WHERE username = ? AND acc_id = ?', [username, acc_id]);
            for (let i = 0; i < result.length; i++) {
                const row = result[i];
                const tResult = await db.query('SELECT * FROM Transactions WHERE tid = ?', [row.tid]);
                if (tResult.length > 0) {
                    let transaction = {
                        id: row.tid,
                        dateExecuted: tResult[0][0].date_executed ? tResult[0][0].date_executed.toISOString().slice(0, 10) : 'Unknown Date',
                        type: null,
                        details: {},
                    };
                    const buyResult = await db.query('SELECT * FROM Buy WHERE tid = ?', [row.tid]);
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
    router.get('/get-movie-info', async (req, res) => {
        let { username, acc_id, balance, position, stockData, movies, stocks, stockInfo, movieInfo, title  } = req.query;
        try {
            stockData = JSON.parse(stockData);
            movies = JSON.parse(movies);
            stocks = JSON.parse(stocks);
            stockInfo = JSON.parse(stockInfo);
        } catch (error) {}
        try {
            const [movieReviews] = await db.query('SELECT * FROM Review WHERE title = ?', [title]);
            if (movieReviews.length > 0) {
                console.log(movieReviews);
                console.log(stockInfo);
         
                res.render('traderInterface', { menu: 'research', username, acc_id, balance, position, stockData, movies, stocks, stockInfo, movieInfo: movieReviews});
            } else {
                res.render('traderInterface', { menu: 'research', error: 'No reviews found for this movie.',});
            }
        } catch (error) {console.error('Error fetching movie info:', error);
            res.status(500).send('Internal Server Error');
        }
    });
    router.get('/get-stock-info', async (req, res) => {
        let { username, acc_id, balance, position, stockData, movies, stocks, movieInfo, symbol  } = req.query;
        try {
            stockData = JSON.parse(stockData);
            movies = JSON.parse(movies);
            stocks = JSON.parse(stocks);
            movieInfo = JSON.parse(movieInfo);
        } catch (error) {}
        try {
            const [stockInfo] = await db.query('SELECT * FROM stock_actor WHERE symbol = ?', [symbol]);
            if (stockInfo.length > 0) {
                console.log(stockInfo);
                console.log(movieInfo);
                res.render('traderInterface', { menu: 'research', username, acc_id, balance, position, stockData, movies, stocks, stockInfo: stockInfo, movieInfo});
            }
        } catch (error) {console.error('Error fetching movie info:', error);
            res.status(500).send('Internal Server Error');
        }
    });
    return router;
}