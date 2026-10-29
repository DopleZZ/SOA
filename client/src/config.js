module.exports = {
    port: process.env.PORT || 3000,
    vehicleServiceBaseUrl: process.env.VEHICLE_SERVICE_BASE_URL || 'http://localhost:8080',
    shopServiceBaseUrl: process.env.SHOP_SERVICE_BASE_URL || 'http://localhost:8080',
    vehicleServiceCaPath: process.env.VEHICLE_SERVICE_CA,
    shopServiceCaPath: process.env.SHOP_SERVICE_CA,
    rejectUnauthorized: process.env.TLS_REJECT_UNAUTHORIZED !== 'false'
};
