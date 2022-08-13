'use strict';

const Command = require('./command');

class AppendCommand extends Command {
    constructor(localfile, remotefile) {
        super('append');
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

module.exports = AppendCommand;
