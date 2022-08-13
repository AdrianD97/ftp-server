'use strict';

const Task = require("./task");

class AppendTask extends Task {
    constructor(id, description = '') {
        super(id, 'append', description);
    }
};

module.exports = AppendTask;
