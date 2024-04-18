module.exports = function(db) {
    const express = require('express');
    const router = express.Router();


    router.all('/Customer-interface', (req, res) => {
        res.render('interface', { interfaceType: 'Customer' });
    });
    router.all('/Mangager-interface', (req, res) => {
        res.render('interface', { interfaceType: 'Manager' });
    });
    router.all('/demo', async (req, res) => {
        let curr_date;
        let is_open;
        try {
            const [rows] = await db.query(`SELECT * FROM Curr_Time`);
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
