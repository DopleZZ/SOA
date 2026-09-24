module.exports = {
    port: process.env.PORT || 3000,
    vehicleServiceBaseUrl: process.env.VEHICLE_SERVICE_BASE_URL || 'https://localhost:8443',
    shopServiceBaseUrl: process.env.SHOP_SERVICE_BASE_URL || 'https://localhost:8543',
    vehicleServiceCaPath: process.env.VEHICLE_SERVICE_CA,
    shopServiceCaPath: process.env.SHOP_SERVICE_CA,
    rejectUnauthorized: process.env.TLS_REJECT_UNAUTHORIZED !== 'false'
};
