const { XMLParser, XMLBuilder } = require('fast-xml-parser');

const parser = new XMLParser({
    ignoreAttributes: true,
    parseTagValue: true,
    trimValues: true,
    isArray: (name) => name === 'vehicle' || name === 'entry'
});

const builder = new XMLBuilder({
    ignoreAttributes: true,
    format: false
});

function parseXml(xmlString) {
    return parser.parse(xmlString);
}

function buildXml(rootName, value) {
    return builder.build({ [rootName]: value });
}

function asArray(value) {
    if (value === undefined || value === null) {
        return [];
    }
    return Array.isArray(value) ? value : [value];
}

module.exports = { parseXml, buildXml, asArray };
