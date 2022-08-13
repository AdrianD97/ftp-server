'use strict';

const Command = require('./command');

class OpenCommand extends Command {
    constructor(host = 'localhost', port = 21) {
        super('open');
        this._host = host;
        this._port = port;
    }

    get host() {
        return this._host;
    }

    get port() {
        return this._port;
    }

    toString() {
        return `${super.toString()} ${this._host} ${this._port}`;
    }
};

module.exports = OpenCommand;
