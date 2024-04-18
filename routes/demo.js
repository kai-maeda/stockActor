module.exports = function(db) {    
    const express = require('express');
    const router = express.Router();

    router.all('/open-market', async (req, res) => {
        try {
            const [rows] = await db.query(`SELECT is_open FROM curr_time LIMIT 1`);    
            if (rows.length === 0) {return res.status(404).send('Market status not found.');}
            await db.query(`UPDATE curr_time SET is_open = TRUE`);  
            res.redirect('/Demo');  
        } catch (error) {
            console.error(error);
            res.status(500).send('Internal Server Error');
        }
    });
    router.all('/close-market', async (req, res) => {
        try {
            const [rows] = await db.query(`SELECT is_open FROM curr_time LIMIT 1`);    
            if (rows.length === 0) {return res.status(404).send('Market status not found.');}
            await db.query(`UPDATE curr_time SET is_open = FALSE`);   
            res.redirect('/Demo'); 
        } catch (error) {
            console.error(error);
            res.status(500).send('Internal Server Error');
        }
    });
    router.all('/next-day', async(req, res) => {
        try {
            await db.query(`
                UPDATE curr_time 
                SET curr_date = DATE_ADD(curr_date, INTERVAL 1 DAY)
            `);
            res.redirect('/Demo');
        } catch (error) {
            console.error(error);
            res.status(500).send('Internal Server Error');
        }
    });
    
    router.all('/entered-stock',async(req, res) => {
        const symbol  = req.query.symbol;
        const is_open  = req.body.is_open;
        let curr_date  = req.query.curr_date;
        curr_date = new Date(curr_date);
        try {
            const [rows] = await db.query('SELECT * FROM Stock_Actor WHERE symbol = ?', [symbol]);
            if(rows.length > 0) {
                let { actor_name, closing_price, date_of_birth, current_price, symbol } = rows[0];
                current_price = parseFloat(current_price).toFixed(2);
                res.render('demo', { actor_name, closing_price, date_of_birth, current_price, symbol, is_open, curr_date });
            } else {
                res.render('demo', { error: "No data found for the provided symbol." });
            }
        } catch (error) {}
    });
    router.all('/change-price', async (req, res) => {
        const symbol = req.body.symbol; 
        let newPrice = req.body.setPrice; 
        const actor_name = req.body.actor_name; 
        const closing_price = req.body.closing_price; 
        const date_of_birth = req.body.date_of_birth; 
        const is_open = req.body.is_open; 
        let curr_date = req.body.curr_date; 
        curr_date = new Date(curr_date);
        newPrice = parseFloat(newPrice).toFixed(2);
        const current_price = newPrice;
        if (!symbol || !newPrice) {
            return res.status(400).send("Symbol and new price are required.");
        }
        try {
            const sql = 'UPDATE Stock_Actor SET current_price = ? WHERE symbol = ?';
            await db.query(sql, [newPrice, symbol]);
            res.render('demo', { actor_name, closing_price, date_of_birth, current_price, symbol, is_open, curr_date });
        } catch (error) { }
    });
    
    

    return router;
}