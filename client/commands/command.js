'use strict';

class Command {
    constructor(name) {
        if (this.constructor === Command) {
            throw new Error('Abstract class \'Command\' can\'t be instantiated');
        }

        this._name = name;
    }

    get name() {
        return this._name;
    }

    toString() {
        return this._name;
    }
};

module.exports = Command;
