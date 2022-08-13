'use strict';

const Command = require('./command');

class UserCommand extends Command {
    constructor(username, password) {
        super('user');
        this._username = username;
        this._password = password;
    }

    get username() {
        return this._username;
    }

    get password() {
        return this._password;
    }

    toString() {
        return `${super.toString()} ${this._username} ${this._password}`;
    }
};

module.exports = UserCommand;
