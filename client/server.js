const path = require('path');
const express = require('express');
const config = require('./src/config');
const vehiclesRouter = require('./src/routes/vehicles');
const shopRouter = require('./src/routes/shop');

const app = express();

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

app.use('/', vehiclesRouter);
app.use('/shop', shopRouter);

app.use((req, res) => {
    res.status(404).render('error', { title: 'Страница не найдена', status: 404, message: 'Такой страницы не существует', path: req.path });
});

app.use((err, req, res, next) => {
    res.status(500).render('error', { title: 'Внутренняя ошибка клиента', status: 500, message: err.message, path: req.path });
});

app.listen(config.port, () => {
    console.log(`Клиент запущен на http://localhost:${config.port}`);
});
