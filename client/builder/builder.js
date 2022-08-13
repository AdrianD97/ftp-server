'use strict';

class Builder {
    constructor() {
        if (this.constructor === Builder) {
            throw new Error('Abstract class \'Builder\' can\'t be instantiated');
        }
    }
};

module.exports = Builder;
