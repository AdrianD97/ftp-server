'use strict';

const Task = require("./task");

class UploadTask extends Task {
    constructor(id, description = '') {
        super(id, 'upload', description);
    }
};

module.exports = UploadTask;
