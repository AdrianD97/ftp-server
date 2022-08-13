'use strict';

const Command = require('./command');

class GetCommand extends Command {
    constructor(remotefile, localfile) {
        super('get');
        this._remotefile = remotefile;
        this._localfile = localfile;
    }

    get remotefile() {
        return this._remotefile;
    }

    get localfile() {
        return this._localfile;
    }

    toString() {
        return `${super.toString()} ${this._remotefile} ${this._localfile}`;
    }
};

module.exports = GetCommand;
