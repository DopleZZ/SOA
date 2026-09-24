const { asArray } = require('../xml');

function toVehicle(node) {
    if (!node) {
        return null;
    }
    return {
        id: node.id,
        name: node.name,
        coordinates: { x: node.coordinates?.x, y: node.coordinates?.y },
        creationDate: node.creationDate,
        enginePower: node.enginePower,
        type: node.type,
        fuelType: node.fuelType
    };
}

function extractVehicleList(parsed) {
    const container = parsed?.collection ?? parsed;
    return asArray(container?.Vehicle ?? container?.vehicle).map(toVehicle);
}

module.exports = { toVehicle, extractVehicleList };
