module.exports = function(db) {
    const express = require('express');
    const router = express.Router();


    router.all('/customer-interface', (req, res) => {
        res.render('interface', { interfaceType: 'customer' });
    });
    router.all('/mangager-interface', (req, res) => {
        res.render('interface', { interfaceType: 'manager' });
    });
    router.all('/demo', async (req, res) => {
        let curr_date;
        let is_open;
        try {
            const [rows] = await db.query(`SELECT * FROM curr_time`);
            if (rows[0].count > 1) {
                return res.status(400).send('There should not be more than one curr_date');
            }
            curr_date = new Date(rows[0].curr_date);
            is_open = rows[0].is_open;
        } catch (error) {}
        res.render('demo', {curr_date, is_open});
    });
    return router;
}
