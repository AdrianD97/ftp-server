'use strict';

const Command = require('./command');

class LsCommand extends Command {
    constructor(file) {
        super('ls');
        this._file = file;
    }

    get file() {
        return this._file;
    }

    toString() {
        return `${super.toString()} ${this._file}`;
    }
};

module.exports = LsCommand;
