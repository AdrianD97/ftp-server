'use strict';

const util = require('util');
const execFile = util.promisify(require('child_process').execFile);
const { hrtime } = require('process');

const NS_PER_SEC = 1e9;

class Runner {
    constructor(scriptPath) {
        this._scriptPath = scriptPath;
    }

    async execute(task) {
        const convertToSeconds = (hrTime) => {
            const result = (hrTime[0] * NS_PER_SEC + hrTime[1]) / NS_PER_SEC;

            return Number(result.toFixed(3));
        };

        const time = hrtime();
        await execFile(this._scriptPath, [task.toString()]);
        return convertToSeconds(hrtime(time));
    }
};

module.exports = Runner;
