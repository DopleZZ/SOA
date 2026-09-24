const config = require('../config');
const { vehicleServiceAgent } = require('../httpAgents');
const { callXmlApi } = require('../apiClient');
const { buildXml, asArray } = require('../xml');
const { toVehicle, extractVehicleList } = require('./common');

const BASE_URL = config.vehicleServiceBaseUrl;
const AGENT = vehicleServiceAgent;

function vehicleInputXml(fields) {
    const payload = {};
    if (fields.name !== undefined) payload.name = fields.name;
    if (fields.x !== undefined || fields.y !== undefined) {
        payload.coordinates = { x: fields.x, y: fields.y };
    }
    if (fields.enginePower !== undefined) payload.enginePower = fields.enginePower;
    if (fields.type !== undefined) payload.type = fields.type;
    if (fields.fuelType !== undefined) payload.fuelType = fields.fuelType;
    return buildXml('Vehicle', payload);
}

async function listVehicles({ pageNumber, pageSize, sort, filter }) {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'GET',
        path: '/api/vehicles',
        query: { pageNumber, pageSize, sort, filter }
    });
    const page = data?.VehiclePage ?? {};
    return {
        pageNumber: page.pageNumber,
        pageSize: page.pageSize,
        totalElements: page.totalElements,
        totalPages: page.totalPages,
        items: asArray(page.items?.vehicle).map(toVehicle)
    };
}

async function getVehicle(id) {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'GET',
        path: `/api/vehicles/${id}`
    });
    return toVehicle(data?.Vehicle);
}

async function createVehicle(fields) {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'POST',
        path: '/api/vehicles',
        body: vehicleInputXml(fields)
    });
    return toVehicle(data?.Vehicle);
}

async function updateVehicle(id, fields) {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'PUT',
        path: `/api/vehicles/${id}`,
        body: vehicleInputXml(fields)
    });
    return toVehicle(data?.Vehicle);
}

async function patchVehicle(id, fields) {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'PATCH',
        path: `/api/vehicles/${id}`,
        body: vehicleInputXml(fields)
    });
    return toVehicle(data?.Vehicle);
}

async function deleteVehicle(id) {
    await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'DELETE',
        path: `/api/vehicles/${id}`
    });
}

async function sumEnginePower() {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'GET',
        path: '/api/vehicles/sum-engine-power'
    });
    return data?.SumResult?.sum;
}

async function groupCountById() {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'GET',
        path: '/api/vehicles/group-count-by-id'
    });
    return asArray(data?.IdGroupCount?.entries?.entry).map((entry) => ({ id: entry.id, count: entry.count }));
}

async function vehiclesByTypeGreater(type) {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'GET',
        path: `/api/vehicles/by-type-greater/${type}`
    });
    return extractVehicleList(data);
}

module.exports = {
    listVehicles,
    getVehicle,
    createVehicle,
    updateVehicle,
    patchVehicle,
    deleteVehicle,
    sumEnginePower,
    groupCountById,
    vehiclesByTypeGreater
};
