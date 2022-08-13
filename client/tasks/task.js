'use strict';

class Task {
    constructor(id, name, description = '') {
        if (this.constructor === Task) {
            throw new Error('Abstract class \'Task\' can\'t be instantiated');
        }

        this._id = id;
        this._name = name;
        this._description = description;
        this._commands = [];
    }

    addCommand(command) {
        this._commands.push(command);
    }

    get id() {
        return this._id;
    }

    get name() {
        return this._name;
    }

    get description() {
        return this._description;
    }

    get commands() {
        return this._commands;
    }

    toString() {
        let task = "";

        this._commands.forEach((cmd) => {
            task += `${cmd.toString()}\\n`;
        });

        return task;
    }
};

module.exports = Task;
