const express = require('express');
const shopService = require('../services/shopService');
const { ApiError } = require('../apiClient');
const { VEHICLE_TYPES } = require('../constants');

const router = express.Router();

router.get('/', (req, res) => {
    res.render('shop', { types: VEHICLE_TYPES, byType: null, byRange: null, errors: {}, query: req.query });
});

router.get('/by-type', async (req, res) => {
    const errors = {};
    let byType = null;
    if (req.query.type) {
        try {
            byType = await shopService.searchByType(req.query.type);
        } catch (err) {
            errors.byType = err instanceof ApiError ? err : new ApiError(500, 'Ошибка', err.message, req.path);
        }
    }
    res.render('shop', { types: VEHICLE_TYPES, byType, byRange: null, errors, query: req.query });
});

router.get('/by-engine-power', async (req, res) => {
    const errors = {};
    let byRange = null;
    if (req.query.from !== undefined && req.query.to !== undefined && req.query.from !== '' && req.query.to !== '') {
        try {
            byRange = await shopService.searchByEnginePowerRange(req.query.from, req.query.to);
        } catch (err) {
            errors.byRange = err instanceof ApiError ? err : new ApiError(500, 'Ошибка', err.message, req.path);
        }
    }
    res.render('shop', { types: VEHICLE_TYPES, byType: null, byRange, errors, query: req.query });
});

module.exports = router;
