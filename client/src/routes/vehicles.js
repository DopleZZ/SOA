const express = require('express');
const vehicleService = require('../services/vehicleService');
const { ApiError } = require('../apiClient');
const { FIELDS, OPERATORS, VEHICLE_TYPES, FUEL_TYPES, FILTER_ROWS, SORT_ROWS } = require('../constants');

const router = express.Router();

function readRows(query, prefix, count, keys) {
    const rows = [];
    for (let i = 0; i < count; i++) {
        const row = {};
        let hasValue = false;
        for (const key of keys) {
            const value = query[`${prefix}_${key}_${i}`] || '';
            row[key] = value;
            if (value) hasValue = true;
        }
        if (hasValue) rows.push(row);
    }
    return rows;
}

function buildFilterParams(query) {
    return readRows(query, 'filter', FILTER_ROWS, ['field', 'op', 'value'])
        .filter((row) => row.field && row.op && row.value !== '')
        .map((row) => `${row.field}:${row.op}:${row.value}`);
}

function buildSortParams(query) {
    return readRows(query, 'sort', SORT_ROWS, ['field', 'dir'])
        .filter((row) => row.field)
        .map((row) => `${row.field},${row.dir || 'asc'}`);
}

function buildQueryLink(query, overrides) {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries({ ...query, ...overrides })) {
        if (value === undefined || value === null || value === '') continue;
        params.set(key, value);
    }
    return '/?' + params.toString();
}

router.get('/', async (req, res) => {
    const pageNumber = parseInt(req.query.pageNumber, 10) || 1;
    const pageSize = parseInt(req.query.pageSize, 10) || 10;
    const filter = buildFilterParams(req.query);
    const sort = buildSortParams(req.query);

    try {
        const page = await vehicleService.listVehicles({ pageNumber, pageSize, sort, filter });
        res.render('index', {
            page,
            query: req.query,
            fields: FIELDS,
            operators: OPERATORS,
            filterRows: FILTER_ROWS,
            sortRows: SORT_ROWS,
            prevLink: pageNumber > 1 ? buildQueryLink(req.query, { pageNumber: pageNumber - 1 }) : null,
            nextLink: page.totalPages && pageNumber < page.totalPages ? buildQueryLink(req.query, { pageNumber: pageNumber + 1 }) : null,
            apiError: null
        });
    } catch (err) {
        const apiError = err instanceof ApiError ? err : new ApiError(500, 'Внутренняя ошибка клиента', err.message, req.path);
        res.status(apiError.status || 500).render('index', {
            page: { items: [], pageNumber, pageSize, totalElements: 0, totalPages: 0 },
            query: req.query,
            fields: FIELDS,
            operators: OPERATORS,
            filterRows: FILTER_ROWS,
            sortRows: SORT_ROWS,
            prevLink: null,
            nextLink: null,
            apiError
        });
    }
});

router.get('/vehicles/new', (req, res) => {
    res.render('vehicle-form', { vehicle: null, mode: 'create', types: VEHICLE_TYPES, fuelTypes: FUEL_TYPES, apiError: null });
});

router.post('/vehicles', async (req, res) => {
    try {
        await vehicleService.createVehicle(readVehicleFields(req.body));
        res.redirect('/');
    } catch (err) {
        const apiError = err instanceof ApiError ? err : new ApiError(500, 'Внутренняя ошибка клиента', err.message, req.path);
        res.status(apiError.status || 500).render('vehicle-form', {
            vehicle: req.body, mode: 'create', types: VEHICLE_TYPES, fuelTypes: FUEL_TYPES, apiError
        });
    }
});

router.get('/vehicles/:id/edit', async (req, res) => {
    try {
        const vehicle = await vehicleService.getVehicle(req.params.id);
        res.render('vehicle-form', { vehicle, mode: 'edit', types: VEHICLE_TYPES, fuelTypes: FUEL_TYPES, apiError: null });
    } catch (err) {
        const apiError = err instanceof ApiError ? err : new ApiError(500, 'Внутренняя ошибка клиента', err.message, req.path);
        res.status(apiError.status || 500).render('error', { title: 'Не удалось загрузить объект', status: apiError.status, message: apiError.detail || apiError.title, path: apiError.path });
    }
});

router.post('/vehicles/:id', async (req, res) => {
    try {
        await vehicleService.updateVehicle(req.params.id, readVehicleFields(req.body));
        res.redirect('/');
    } catch (err) {
        const apiError = err instanceof ApiError ? err : new ApiError(500, 'Внутренняя ошибка клиента', err.message, req.path);
        res.status(apiError.status || 500).render('vehicle-form', {
            vehicle: { id: req.params.id, ...req.body }, mode: 'edit', types: VEHICLE_TYPES, fuelTypes: FUEL_TYPES, apiError
        });
    }
});

router.post('/vehicles/:id/patch', async (req, res) => {
    const fields = {};
    for (const key of ['name', 'x', 'y', 'enginePower', 'type', 'fuelType']) {
        if (req.body[key] !== undefined && req.body[key] !== '') {
            fields[key] = req.body[key];
        }
    }
    try {
        await vehicleService.patchVehicle(req.params.id, fields);
        res.redirect('/');
    } catch (err) {
        const apiError = err instanceof ApiError ? err : new ApiError(500, 'Внутренняя ошибка клиента', err.message, req.path);
        res.status(apiError.status || 500).render('vehicle-form', {
            vehicle: { id: req.params.id, ...req.body }, mode: 'edit', types: VEHICLE_TYPES, fuelTypes: FUEL_TYPES, apiError
        });
    }
});

router.post('/vehicles/:id/delete', async (req, res) => {
    try {
        await vehicleService.deleteVehicle(req.params.id);
        res.redirect('/');
    } catch (err) {
        const apiError = err instanceof ApiError ? err : new ApiError(500, 'Внутренняя ошибка клиента', err.message, req.path);
        res.status(apiError.status || 500).render('error', { title: 'Не удалось удалить объект', status: apiError.status, message: apiError.detail || apiError.title, path: apiError.path });
    }
});

router.get('/stats', async (req, res) => {
    const result = { sum: null, groups: null, greaterThan: null, types: VEHICLE_TYPES };
    const errors = {};

    try {
        result.sum = await vehicleService.sumEnginePower();
    } catch (err) {
        errors.sum = err instanceof ApiError ? err : new ApiError(500, 'Ошибка', err.message, req.path);
    }

    try {
        result.groups = await vehicleService.groupCountById();
    } catch (err) {
        errors.groups = err instanceof ApiError ? err : new ApiError(500, 'Ошибка', err.message, req.path);
    }

    if (req.query.type) {
        try {
            result.greaterThan = await vehicleService.vehiclesByTypeGreater(req.query.type);
        } catch (err) {
            errors.greaterThan = err instanceof ApiError ? err : new ApiError(500, 'Ошибка', err.message, req.path);
        }
    }

    res.render('stats', { result, errors, selectedType: req.query.type || '' });
});

function readVehicleFields(body) {
    return {
        name: body.name,
        x: body.x,
        y: body.y,
        enginePower: body.enginePower,
        type: body.type,
        fuelType: body.fuelType
    };
}

module.exports = router;
