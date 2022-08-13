'use strict';

const Task = require("./task");

class DownloadTask extends Task {
    constructor(id, description = '') {
        super(id, 'download', description);
    }
};

module.exports = DownloadTask;
