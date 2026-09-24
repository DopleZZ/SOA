const { request } = require('undici');
const { parseXml } = require('./xml');

class ApiError extends Error {
    constructor(status, title, detail, path) {
        super(detail || title);
        this.status = status;
        this.title = title;
        this.detail = detail;
        this.path = path;
    }
}

async function callXmlApi({ baseUrl, agent, method, path, query, body }) {
    const url = new URL(path, baseUrl);
    if (query) {
        for (const [key, values] of Object.entries(query)) {
            for (const value of [].concat(values).filter((v) => v !== undefined && v !== null && v !== '')) {
                url.searchParams.append(key, value);
            }
        }
    }

    let response;
    try {
        response = await request(url, {
            method,
            dispatcher: agent,
            headers: body ? { 'content-type': 'application/xml', accept: 'application/xml' } : { accept: 'application/xml' },
            body
        });
    } catch (err) {
        const reason = err.cause?.message || err.message || String(err);
        throw new ApiError(0, 'Сервис недоступен', 'Не удалось установить соединение: ' + reason, path);
    }

    const rawBody = await response.body.text();
    const status = response.statusCode;

    if (status >= 200 && status < 300) {
        return { status, data: rawBody ? parseXml(rawBody) : null };
    }

    let title = 'Ошибка';
    let detail = rawBody;
    let errorPath = path;
    if (rawBody) {
        try {
            const parsed = parseXml(rawBody);
            const errorNode = parsed.Error || parsed;
            if (errorNode) {
                title = errorNode.error || title;
                detail = errorNode.message || detail;
                errorPath = errorNode.path || errorPath;
            }
        } catch (err) {
            detail = rawBody;
        }
    }
    throw new ApiError(status, title, detail, errorPath);
}

module.exports = { callXmlApi, ApiError };
