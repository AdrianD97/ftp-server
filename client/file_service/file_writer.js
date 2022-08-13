'use strict';

class FileWriter {
    constructor() {
        if (this.constructor === FileWriter) {
            throw new Error('Abstract class \'FileWriter\' can\'t be instantiated');
        }
    }

    write(filename, data) {
        throw new Error('Abstract method has no implementation');
    }
};

module.exports = FileWriter;
