const config = require('../config');
const { shopServiceAgent } = require('../httpAgents');
const { callXmlApi } = require('../apiClient');
const { extractVehicleList } = require('./common');

const BASE_URL = config.shopServiceBaseUrl;
const AGENT = shopServiceAgent;

async function searchByType(type) {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'GET',
        path: `/shop/search/by-type/${type}`
    });
    return extractVehicleList(data);
}

async function searchByEnginePowerRange(from, to) {
    const { data } = await callXmlApi({
        baseUrl: BASE_URL,
        agent: AGENT,
        method: 'GET',
        path: `/shop/search/by-engine-power/${from}/${to}`
    });
    return extractVehicleList(data);
}

module.exports = { searchByType, searchByEnginePowerRange };
