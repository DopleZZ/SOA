'use strict';

(function () {
    const FIELDS = ['id', 'name', 'coordinates.x', 'coordinates.y', 'creationDate', 'enginePower', 'type', 'fuelType'];
    const OPERATORS = ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'like'];
    const VEHICLE_TYPES = ['PLANE', 'HELICOPTER', 'SPACESHIP'];
    const FUEL_TYPES = ['ELECTRICITY', 'MANPOWER', 'ANTIMATTER'];
    const SORT_ROWS = 2;


    const defaults = (window.APP_CONFIG || {});
    const settings = {
        vehicleBase: localStorage.getItem('vehicleBase') || defaults.vehicleServiceBaseUrl || 'https://localhost:8443',
        shopBase: localStorage.getItem('shopBase') || defaults.shopServiceBaseUrl || 'https://localhost:8543'
    };


    class ApiError extends Error {
        constructor(status, title, detail, path) {
            super(detail || title);
            this.status = status;
            this.title = title;
            this.detail = detail;
            this.path = path;
        }
    }


    function esc(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function vehicleXml(fields) {
        const parts = [];
        if (fields.name !== undefined) parts.push(`<name>${esc(fields.name)}</name>`);
        if (fields.x !== undefined || fields.y !== undefined) {
            parts.push(`<coordinates><x>${esc(fields.x)}</x><y>${esc(fields.y)}</y></coordinates>`);
        }
        if (fields.enginePower !== undefined) parts.push(`<enginePower>${esc(fields.enginePower)}</enginePower>`);
        if (fields.type !== undefined) parts.push(`<type>${esc(fields.type)}</type>`);
        if (fields.fuelType !== undefined) parts.push(`<fuelType>${esc(fields.fuelType)}</fuelType>`);
        return `<Vehicle>${parts.join('')}</Vehicle>`;
    }

    function parseXml(text) {
        const doc = new DOMParser().parseFromString(text, 'application/xml');
        if (doc.getElementsByTagName('parsererror').length > 0) {
            throw new ApiError(0, 'Некорректный XML в ответе сервиса', text.slice(0, 500), '');
        }
        return doc;
    }

    function text(parent, tag) {
        const el = parent.getElementsByTagName(tag)[0];
        return el ? el.textContent : '';
    }

    function vehicleFromXml(el) {
        if (!el || (el.tagName !== 'Vehicle' && el.tagName !== 'vehicle')) return null;
        const coordinates = el.getElementsByTagName('coordinates')[0];
        return {
            id: text(el, 'id'),
            name: text(el, 'name'),
            x: coordinates ? text(coordinates, 'x') : '',
            y: coordinates ? text(coordinates, 'y') : '',
            creationDate: text(el, 'creationDate'),
            enginePower: text(el, 'enginePower'),
            type: text(el, 'type'),
            fuelType: text(el, 'fuelType')
        };
    }

    function vehicleListFromXml(doc) {
        const root = doc.documentElement;
        if (!root) return [];
        const items = (root.tagName === 'collection' || root.tagName === 'list')
            ? Array.from(root.children)
            : [root];
        return items.map(vehicleFromXml).filter(Boolean);
    }

    function pageFromXml(doc) {
        const page = doc.documentElement;
        const itemsEl = page.getElementsByTagName('items')[0];
        const items = itemsEl
            ? Array.from(itemsEl.children).map(vehicleFromXml).filter(Boolean)
            : [];
        return {
            pageNumber: text(page, 'pageNumber'),
            pageSize: text(page, 'pageSize'),
            totalElements: text(page, 'totalElements'),
            totalPages: parseInt(text(page, 'totalPages'), 10) || 0,
            items
        };
    }


    function buildUrl(base, path, query) {
        let url = base.replace(/\/+$/, '') + path;
        if (query) {
            const params = new URLSearchParams();
            for (const [key, values] of Object.entries(query)) {
                for (const value of [].concat(values)) {
                    if (value !== undefined && value !== null && value !== '') params.append(key, value);
                }
            }
            const qs = params.toString();
            if (qs) url += '?' + qs;
        }
        return url;
    }

    async function callXml(base, path, { method = 'GET', body = null, query = null } = {}) {
        let response;
        try {
            response = await fetch(buildUrl(base, path, query), {
                method,
                headers: body
                    ? { 'Accept': 'application/xml', 'Content-Type': 'application/xml' }
                    : { 'Accept': 'application/xml' },
                body: body || undefined
            });
        } catch (err) {
            throw new ApiError(0, 'Сервис недоступен', `Не удалось установить соединение с ${base}: ${err.message}`, path);
        }

        const raw = await response.text();
        if (response.ok) {
            return raw.trim() ? parseXml(raw) : null;
        }

        let title = 'Ошибка';
        let detail = raw;
        let errorPath = path;
        if (raw.trim()) {
            try {
                const doc = parseXml(raw);
                const node = doc.getElementsByTagName('Error')[0] || doc.documentElement;
                if (node) {
                    title = text(node, 'error') || title;
                    detail = text(node, 'message') || detail;
                    errorPath = text(node, 'path') || errorPath;
                }
            } catch (err) {
                detail = raw;
            }
        }
        throw new ApiError(response.status, title, detail, errorPath);
    }


    const api = {
        listVehicles({ pageNumber, pageSize, sort, filter }) {
            return callXml(settings.vehicleBase, '/api/vehicles', {
                query: { pageNumber, pageSize, sort, filter }
            }).then(pageFromXml);
        },
        getVehicle(id) {
            return callXml(settings.vehicleBase, `/api/vehicles/${encodeURIComponent(id)}`)
                .then((doc) => vehicleFromXml(doc.documentElement));
        },
        createVehicle(fields) {
            return callXml(settings.vehicleBase, '/api/vehicles', { method: 'POST', body: vehicleXml(fields) });
        },
        updateVehicle(id, fields) {
            return callXml(settings.vehicleBase, `/api/vehicles/${encodeURIComponent(id)}`, {
                method: 'PUT', body: vehicleXml(fields)
            });
        },
        patchVehicle(id, fields) {
            return callXml(settings.vehicleBase, `/api/vehicles/${encodeURIComponent(id)}`, {
                method: 'PATCH', body: vehicleXml(fields)
            });
        },
        deleteVehicle(id) {
            return callXml(settings.vehicleBase, `/api/vehicles/${encodeURIComponent(id)}`, { method: 'DELETE' });
        },
        sumEnginePower() {
            return callXml(settings.vehicleBase, '/api/vehicles/sum-engine-power')
                .then((doc) => text(doc.documentElement, 'sum'));
        },
        groupCountById() {
            return callXml(settings.vehicleBase, '/api/vehicles/group-count-by-id').then((doc) =>
                Array.from(doc.getElementsByTagName('entry')).map((entry) => ({
                    id: text(entry, 'id'),
                    count: text(entry, 'count')
                }))
            );
        },
        vehiclesByTypeGreater(type) {
            return callXml(settings.vehicleBase, `/api/vehicles/by-type-greater/${encodeURIComponent(type)}`)
                .then(vehicleListFromXml);
        },
        shopSearchByType(type) {
            return callXml(settings.shopBase, `/shop/search/by-type/${encodeURIComponent(type)}`)
                .then(vehicleListFromXml);
        },
        shopSearchByEnginePowerRange(from, to) {
            return callXml(settings.shopBase, `/shop/search/by-engine-power/${encodeURIComponent(from)}/${encodeURIComponent(to)}`)
                .then(vehicleListFromXml);
        }
    };


    const app = document.getElementById('app');
    const globalError = document.getElementById('global-error');

    function hideGlobalError() {
        globalError.hidden = true;
    }

    function showGlobalError(err) {
        globalError.innerHTML = errorHtml(err);
        globalError.hidden = false;
    }

    function errorHtml(err) {
        const status = err instanceof ApiError ? err.status : 500;
        const title = err instanceof ApiError ? err.title : 'Внутренняя ошибка клиента';
        const detail = err instanceof ApiError ? (err.detail || err.title) : String(err && err.message ? err.message : err);
        const path = err instanceof ApiError ? err.path : '';
        const hint = status === 0
            ? '<p>Проверьте адреса сервисов в разделе <a href="#/settings">Настройки</a>. ' +
              'Самоподписанный сертификат нужно принять: откройте адрес сервиса в браузере и подтвердите исключение.</p>'
            : '';
        return `<strong>Ошибка ${status}: ${esc(title)}</strong>` +
            `<p>${esc(detail)}</p>${hint}<span class="path">${esc(path)}</span>`;
    }

    function options(values, selected) {
        return values
            .map((v) => `<option value="${esc(v)}"${v === selected ? ' selected' : ''}>${esc(v)}</option>`)
            .join('');
    }

    const COLUMNS = [
        ['id', 'id'], ['Название', 'name'], ['x', 'coordinates.x'], ['y', 'coordinates.y'],
        ['Создан', 'creationDate'], ['Мощность', 'enginePower'], ['Тип', 'type'], ['Топливо', 'fuelType']
    ];

    function vehicleTable(items, sortable) {
        if (!items.length) {
            return '<p>Ничего не найдено.</p>';
        }
        const rows = items.map((v) => `
            <tr>
                <td>${esc(v.id)}</td>
                <td>${esc(v.name)}</td>
                <td>${esc(v.x)}</td>
                <td>${esc(v.y)}</td>
                <td>${esc(String(v.creationDate).replace('T', ' '))}</td>
                <td>${esc(v.enginePower)}</td>
                <td><span class="badge ${esc(String(v.type).toLowerCase())}">${esc(v.type)}</span></td>
                <td><span class="badge ${esc(String(v.fuelType).toLowerCase())}">${esc(v.fuelType)}</span></td>
                <td class="actions">
                    <a href="#/vehicles/${esc(v.id)}/edit">Изменить</a>
                    <a href="#/vehicles/${esc(v.id)}/patch">Точечно</a>
                    <button type="button" class="danger" data-delete="${esc(v.id)}">Удалить</button>
                </td>
            </tr>`).join('');
        const primary = listState.sort[0] || ['id', 'asc'];
        const head = COLUMNS.map(([title, field]) => {
            if (!sortable) {
                return `<th>${title}</th>`;
            }
            const active = primary[0] === field;
            const arrow = active ? (primary[1] === 'desc' ? ' ↓' : ' ↑') : '';
            const marked = active ? ' sorted' : '';
            return `<th class="sortable${marked}" data-field="${field}">${title}${arrow}</th>`;
        }).join('');
        return `<div class="table-wrap"><table>
            <thead><tr>${head}<th></th></tr></thead>
            <tbody>${rows}</tbody>
        </table></div>`;
    }

    function bindDeleteButtons(container, reload) {
        container.querySelectorAll('button[data-delete]').forEach((button) => {
            button.addEventListener('click', async () => {
                if (!confirm(`Удалить объект ${button.dataset.delete}?`)) return;
                try {
                    await api.deleteVehicle(button.dataset.delete);
                    reload();
                } catch (err) {
                    showGlobalError(err);
                }
            });
        });
    }


    const listState = { pageNumber: 1, pageSize: 10, sort: [['id', 'asc']], filter: [] };

    function sortRowHtml(index, row) {
        const [field = 'id', dir = 'asc'] = row || [];
        return `<div class="row">
            <select name="sort_field_${index}">${options(FIELDS, FIELDS.includes(field) ? field : 'id')}</select>
            <select name="sort_dir_${index}">${options(['asc', 'desc'], dir)}</select>
        </div>`;
    }

    function filterRowHtml(index, row, removable) {
        const [field = '', op = 'eq', value = ''] = row || [];
        const remove = removable
            ? `<button type="button" class="danger" data-remove-row="${index}">✕</button>`
            : '';
        return `<div class="filter-row">
            <div class="row">
                <select name="filter_field_${index}"><option value="">—</option>${options(FIELDS, field)}</select>
                <select name="filter_op_${index}">${options(OPERATORS, op)}</select>
                <input type="text" name="filter_value_${index}" value="${esc(value)}" placeholder="значение">
                ${remove}
            </div>
        </div>`;
    }

    function renderList() {
        app.innerHTML = `
            <section>
                <h1>Гараж</h1>\n                <p class="subtitle">Коллекция транспортных средств: фильтрация, сортировка, постраничный вывод</p>
                <p class="actions-card"><a class="button primary" href="#/vehicles/new">Добавить транспорт</a></p>
                <form id="query-form">
                    <fieldset>
                        <legend>Страница</legend>
                        <div class="row">
                            <label>Номер страницы <input type="number" name="pageNumber" min="1" value="${esc(listState.pageNumber)}" style="width:6em"></label>
                            <label>Размер страницы <input type="number" name="pageSize" min="1" value="${esc(listState.pageSize)}" style="width:6em"></label>
                        </div>
                    </fieldset>
                    <fieldset>
                        <legend>Сортировка (поле, направление)</legend>
                        ${sortRowHtml(0, listState.sort[0])}
                        ${sortRowHtml(1, listState.sort[1])}
                    </fieldset>
                    <fieldset>
                        <legend>Фильтрация</legend>
                        <div id="filter-rows">
                            ${(listState.filter.length ? listState.filter : [['', '', '']])
                                .map((row, i) => filterRowHtml(i, row, listState.filter.length > 1 || i > 0))
                                .join('')}
                        </div>
                        <button type="button" id="add-filter">+ условие</button>
                    </fieldset>
                    <button type="submit" class="primary">Обновить</button>
                </form>
                <div id="list-result"><p>Загрузка…</p></div>
            </section>`;
        document.getElementById('query-form').addEventListener('submit', onQuerySubmit);
        const filterRows = document.getElementById('filter-rows');
        filterRows.addEventListener('click', (event) => {
            const removeButton = event.target.closest('[data-remove-row]');
            if (removeButton) removeButton.closest('.filter-row').remove();
        });
        document.getElementById('add-filter').addEventListener('click', () => {
            filterRows.insertAdjacentHTML('beforeend', filterRowHtml(filterRows.children.length, null, true));
        });
        loadList();
    }

    function onQuerySubmit(event) {
        event.preventDefault();
        const form = event.target;
        const elements = form.elements;
        listState.pageNumber = Math.max(1, parseInt(elements.pageNumber.value, 10) || 1);
        listState.pageSize = Math.max(1, parseInt(elements.pageSize.value, 10) || 10);
        listState.sort = [];
        for (let i = 0; i < SORT_ROWS; i++) {
            const field = elements[`sort_field_${i}`].value;
            if (field) listState.sort.push([field, elements[`sort_dir_${i}`].value]);
        }
        listState.filter = [...form.querySelectorAll('#filter-rows .filter-row')]
            .map((row) => {
                const field = row.querySelector('select[name^="filter_field_"]').value;
                const op = row.querySelector('select[name^="filter_op_"]').value;
                const value = row.querySelector('input[name^="filter_value_"]').value.trim();
                return field && value !== '' ? [field, op, value] : null;
            })
            .filter(Boolean);
        loadList();
    }

    function toRsql(field, op, value) {
        if (op === 'like') return `${field}==*${value}*`;
        if (op === 'eq') return `${field}==${value}`;
        if (op === 'ne') return `${field}!=${value}`;
        const symbols = { gt: 'gt', gte: 'ge', lt: 'lt', lte: 'le' };
        return `${field}=${symbols[op]}=${value}`;
    }

    async function loadList() {
        const result = document.getElementById('list-result');
        if (!result) return;
        result.innerHTML = '<p>Загрузка…</p>';
        try {
            const page = await api.listVehicles({
                pageNumber: listState.pageNumber,
                pageSize: listState.pageSize,
                sort: listState.sort.map(([field, dir]) => `${field},${dir}`),
                filter: [listState.filter.map(([field, op, value]) => toRsql(field, op, value)).join(';')]
            });
            if (page.totalPages > 0 && listState.pageNumber > page.totalPages) {
                listState.pageNumber = 1;
                return loadList();
            }
            const total = page.totalPages || 0;
            const prev = listState.pageNumber > 1
                ? '<button type="button" id="prev-page">← Назад</button>' : '';
            const next = total && listState.pageNumber < total
                ? '<button type="button" id="next-page">Вперёд →</button>' : '';
            result.innerHTML = `
                ${vehicleTable(page.items, true)}
                <p class="muted">Страница ${esc(page.pageNumber)} из ${esc(total)},
                всего объектов: ${esc(page.totalElements)}</p>
                <p>${prev} ${next}</p>`;
            const prevButton = document.getElementById('prev-page');
            const nextButton = document.getElementById('next-page');
            if (prevButton) prevButton.addEventListener('click', () => {
                listState.pageNumber -= 1;
                renderList();
            });
            if (nextButton) nextButton.addEventListener('click', () => {
                listState.pageNumber += 1;
                renderList();
            });
            result.querySelectorAll('th.sortable').forEach((th) => {
                th.addEventListener('click', () => {
                    const field = th.dataset.field;
                    const current = listState.sort[0] || ['id', 'asc'];
                    const direction = current[0] === field && current[1] === 'asc' ? 'desc' : 'asc';
                    listState.sort = [[field, direction]];
                    listState.pageNumber = 1;
                    renderList();
                });
            });
            bindDeleteButtons(result, loadList);
        } catch (err) {
            result.innerHTML = '';
            showGlobalError(err);
        }
    }


    function vehicleFieldsHtml(v, patch) {
        const value = (field) => (v && v[field] !== undefined && v[field] !== null ? v[field] : '');
        return `
            <label>Название <input type="text" name="name" value="${esc(value('name'))}"${patch ? '' : ' required'}></label>
            <div class="row">
                <label>x <input type="text" name="x" value="${esc(value('x'))}"${patch ? '' : ' required'}></label>
                <label>y (целое, &gt; -575) <input type="text" name="y" value="${esc(value('y'))}"${patch ? '' : ' required'}></label>
            </div>
            <label>Мощность двигателя (&gt; 0) <input type="text" name="enginePower" value="${esc(value('enginePower'))}"${patch ? '' : ' required'}></label>
            <label>Тип <select name="type">${patch ? '<option value="">— не менять —</option>' : ''}${options(VEHICLE_TYPES, value('type'))}</select></label>
            <label>Тип топлива <select name="fuelType">${patch ? '<option value="">— не менять —</option>' : ''}${options(FUEL_TYPES, value('fuelType'))}</select></label>`;
    }

    function readFormFields(form, patch) {
        const el = form.elements;
        if (patch) {
            const fields = {};
            if (el.name.value.trim() !== '') fields.name = el.name.value.trim();
            if (el.x.value.trim() !== '' || el.y.value.trim() !== '') {
                fields.x = el.x.value.trim();
                fields.y = el.y.value.trim();
            }
            if (el.enginePower.value.trim() !== '') fields.enginePower = el.enginePower.value.trim();
            if (el.type.value) fields.type = el.type.value;
            if (el.fuelType.value) fields.fuelType = el.fuelType.value;
            return fields;
        }
        return {
            name: el.name.value.trim(),
            x: el.x.value.trim(),
            y: el.y.value.trim(),
            enginePower: el.enginePower.value.trim(),
            type: el.type.value,
            fuelType: el.fuelType.value
        };
    }

    function renderVehicleForm(mode, id) {
        const title = mode === 'create' ? 'Новый транспорт' : `Изменение транспорта № ${esc(id)}`;
        app.innerHTML = `
            <section>
                <h1>${title}</h1>
                <form id="vehicle-form">
                    <div id="form-error"></div>
                    <div id="form-fields">Загрузка…</div>
                    <button type="submit" class="primary">${mode === 'create' ? 'Создать' : 'Сохранить'}</button>
                    <a href="#/">Отмена</a>
                </form>
            </section>`;
        const form = document.getElementById('vehicle-form');

        const fill = (vehicle) => {
            document.getElementById('form-fields').innerHTML =
                `<fieldset>${vehicleFieldsHtml(vehicle, false)}</fieldset>`;
            form.addEventListener('submit', async (event) => {
                event.preventDefault();
                const fields = readFormFields(form, false);
                try {
                    if (mode === 'create') await api.createVehicle(fields);
                    else await api.updateVehicle(id, fields);
                    location.hash = '#/';
                } catch (err) {
                    document.getElementById('form-error').innerHTML = errorHtml(err);
                }
            });
        };

        if (mode === 'create') {
            fill(null);
        } else {
            api.getVehicle(id).then(fill).catch((err) => {
                document.getElementById('form-fields').innerHTML = '';
                showGlobalError(err);
            });
        }
    }

    function renderPatchForm(id) {
        app.innerHTML = `
            <section>
                <h1>Точечное изменение транспорта № ${esc(id)}</h1>
                <p>Заполните только те поля, которые нужно изменить (минимум одно).</p>
                <form id="patch-form">
                    <div id="form-error"></div>
                    <fieldset>${vehicleFieldsHtml(null, true)}</fieldset>
                    <button type="submit" class="primary">Применить</button>
                    <a href="#/">Отмена</a>
                </form>
            </section>`;
        const form = document.getElementById('patch-form');
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const fields = readFormFields(form, true);
            if (Object.keys(fields).length === 0) {
                document.getElementById('form-error').innerHTML =
                    '<strong>Заполните хотя бы одно поле.</strong>';
                return;
            }
            try {
                await api.patchVehicle(id, fields);
                location.hash = '#/';
            } catch (err) {
                document.getElementById('form-error').innerHTML = errorHtml(err);
            }
        });
    }


    function renderStats() {
        app.innerHTML = `
            <section>
                <h1>Статистика</h1>
                <div id="stats-sum"><p>Загрузка…</p></div>
                <div id="stats-groups"><p>Загрузка…</p></div>
                <form id="greater-form">
                    <h2>Транспорт с типом больше заданного</h2>
                    <div id="greater-error"></div>
                    <div class="row">
                        <select name="type">${options(VEHICLE_TYPES, 'PLANE')}</select>
                        <button type="submit" class="primary">Показать</button>
                    </div>
                </form>
                <div id="greater-result"></div>
            </section>`;

        api.sumEnginePower()
            .then((sum) => {
                document.getElementById('stats-sum').innerHTML =
                    `<h2>Суммарная мощность двигателя</h2><p class="stat">${esc(sum)}</p>`;
            })
            .catch((err) => {
                document.getElementById('stats-sum').innerHTML = errorHtml(err);
            });

        api.groupCountById()
            .then((groups) => {
                const rows = groups.map((g) => `<tr><td>${esc(g.id)}</td><td>${esc(g.count)}</td></tr>`).join('');
                document.getElementById('stats-groups').innerHTML = `
                    <h2>Количество объектов по id</h2>
                    ${groups.length
                        ? `<table><thead><tr><th>id</th><th>Количество</th></tr></thead><tbody>${rows}</tbody></table>`
                        : '<p>Коллекция пуста.</p>'}`;
            })
            .catch((err) => {
                document.getElementById('stats-groups').innerHTML = errorHtml(err);
            });

        document.getElementById('greater-form').addEventListener('submit', async (event) => {
            event.preventDefault();
            const type = event.target.elements.type.value;
            document.getElementById('greater-result').innerHTML = '<p>Загрузка…</p>';
            try {
                const items = await api.vehiclesByTypeGreater(type);
                document.getElementById('greater-result').innerHTML =
                    `<h2>Транспорт с типом больше ${esc(type)}</h2>${vehicleTable(items)}`;
                bindDeleteButtons(document.getElementById('greater-result'), renderStats);
            } catch (err) {
                document.getElementById('greater-error').innerHTML = errorHtml(err);
                document.getElementById('greater-result').innerHTML = '';
            }
        });
    }


    function renderShop() {
        app.innerHTML = `
            <section>
                <h1>Магазин</h1>
                <form id="shop-type-form">
                    <h2>Поиск по типу</h2>
                    <div id="shop-type-error"></div>
                    <div class="row">
                        <select name="type">${options(VEHICLE_TYPES, 'PLANE')}</select>
                        <button type="submit" class="primary">Найти</button>
                    </div>
                </form>
                <div id="shop-type-result"></div>
                <form id="shop-range-form">
                    <h2>Поиск по диапазону мощности двигателя</h2>
                    <div id="shop-range-error"></div>
                    <div class="row">
                        <label>От <input type="text" name="from" required></label>
                        <label>До <input type="text" name="to" required></label>
                        <button type="submit" class="primary">Найти</button>
                    </div>
                </form>
                <div id="shop-range-result"></div>
            </section>`;

        document.getElementById('shop-type-form').addEventListener('submit', async (event) => {
            event.preventDefault();
            const type = event.target.elements.type.value;
            document.getElementById('shop-type-result').innerHTML = '<p>Загрузка…</p>';
            document.getElementById('shop-type-error').innerHTML = '';
            try {
                const items = await api.shopSearchByType(type);
                document.getElementById('shop-type-result').innerHTML =
                    `<h2>Транспорт типа ${esc(type)}</h2>${vehicleTable(items)}`;
                bindDeleteButtons(document.getElementById('shop-type-result'), renderShop);
            } catch (err) {
                document.getElementById('shop-type-error').innerHTML = errorHtml(err);
                document.getElementById('shop-type-result').innerHTML = '';
            }
        });

        document.getElementById('shop-range-form').addEventListener('submit', async (event) => {
            event.preventDefault();
            const from = event.target.from.value.trim();
            const to = event.target.to.value.trim();
            document.getElementById('shop-range-result').innerHTML = '<p>Загрузка…</p>';
            document.getElementById('shop-range-error').innerHTML = '';
            try {
                const items = await api.shopSearchByEnginePowerRange(from, to);
                document.getElementById('shop-range-result').innerHTML =
                    `<h2>Транспорт с мощностью от ${esc(from)} до ${esc(to)}</h2>${vehicleTable(items)}`;
                bindDeleteButtons(document.getElementById('shop-range-result'), renderShop);
            } catch (err) {
                document.getElementById('shop-range-error').innerHTML = errorHtml(err);
                document.getElementById('shop-range-result').innerHTML = '';
            }
        });
    }


    function renderSettings() {
        app.innerHTML = `
            <section>
                <h1>Настройки</h1>
                <p>Адреса обоих сервисов. Значения сохраняются в localStorage браузера
                и имеют приоритет над <code>config.js</code>.</p>
                <form id="settings-form">
                    <div id="settings-msg"></div>
                    <label>vehicle-service (базовый URL) <input type="text" name="vehicleBase" value="${esc(settings.vehicleBase)}" required></label>
                    <label>shop-service (базовый URL) <input type="text" name="shopBase" value="${esc(settings.shopBase)}" required></label>
                    <button type="submit" class="primary">Сохранить</button>
                </form>
            </section>`;
        document.getElementById('settings-form').addEventListener('submit', (event) => {
            event.preventDefault();
            const form = event.target;
            settings.vehicleBase = form.vehicleBase.value.trim().replace(/\/+$/, '');
            settings.shopBase = form.shopBase.value.trim().replace(/\/+$/, '');
            localStorage.setItem('vehicleBase', settings.vehicleBase);
            localStorage.setItem('shopBase', settings.shopBase);
            document.getElementById('settings-msg').innerHTML =
                '<p><strong>Сохранено.</strong></p>';
        });
    }


    function setActiveNav(path) {
        const section = path === '' || path === '/' ? '/' : '/' + path.split('/')[1];
        document.querySelectorAll('nav a').forEach((a) =>
            a.classList.toggle('active', a.getAttribute('href') === '#' + section));
    }

    function route() {
        const path = (location.hash || '#/').slice(1);
        setActiveNav(path);
        hideGlobalError();
        let match;
        if (path === '/' || path === '') return renderList();
        if (path === '/stats') return renderStats();
        if (path === '/shop') return renderShop();
        if (path === '/settings') return renderSettings();
        if (path === '/vehicles/new') return renderVehicleForm('create');
        if ((match = path.match(/^\/vehicles\/(\d+)\/edit$/))) return renderVehicleForm('edit', match[1]);
        if ((match = path.match(/^\/vehicles\/(\d+)\/patch$/))) return renderPatchForm(match[1]);
        showGlobalError(new ApiError(404, 'Страница не найдена', `Неизвестный маршрут: ${path}`, path));
    }

    window.addEventListener('hashchange', route);
    route();
})();
