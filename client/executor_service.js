'use strict';

const Runner = require('./runner');

class ExecutorService {
    constructor() {
        this._results = new Map();
    }

    async execute(scriptPath, tasks) {
        const createKey = (id, name) => {
            return `${name}__${id}`;
        };
        const runner = new Runner(scriptPath);
        const promises = [];

        tasks.forEach((task) => {
            promises.push(runner.execute(task));
        });

        try {
            const results = await Promise.all(promises);

            for (let i = 0; i < tasks.length; ++i) {
                this._results.set(createKey(tasks[i].id, tasks[i].name), results[i]);
            }
        } catch (e) {
            console.error(e);
        }

        return this._results;
    }
};

module.exports = ExecutorService;
