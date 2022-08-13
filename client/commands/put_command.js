'use strict';

const Command = require('./command');

class PutCommand extends Command {
    constructor(localfile, remotefile) {
        super('put');
        this._localfile = localfile;
        this._remotefile = remotefile;
    }

    get localfile() {
        return this._localfile;
    }

    get remotefile() {
        return this._remotefile;
    }

    toString() {
        return `${super.toString()} ${this._localfile} ${this._remotefile}`;
    }
};

module.exports = PutCommand;
