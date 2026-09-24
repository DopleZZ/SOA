const fs = require('fs');
const { Agent } = require('undici');
const config = require('./config');

function buildAgent(caPath) {
    let ca;
    try {
        ca = fs.readFileSync(caPath);
    } catch (err) {
        ca = undefined;
    }
    return new Agent({
        connect: {
            ca,
            rejectUnauthorized: config.rejectUnauthorized
        }
    });
}

module.exports = {
    vehicleServiceAgent: buildAgent(config.vehicleServiceCaPath),
    shopServiceAgent: buildAgent(config.shopServiceCaPath)
};
