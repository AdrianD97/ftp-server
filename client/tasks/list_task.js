'use strict';

const Task = require("./task");

class ListTask extends Task {
    constructor(id, description = '') {
        super(id, 'list', description);
    }
};

module.exports = ListTask;
