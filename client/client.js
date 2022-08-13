'use strict';

const ExecutorService = require('./executor_service');
const { CSVFileWriter } = require('./file_service');

class Client {
    constructor() {
        if (this.constructor === Client) {
            throw new Error('Abstract class \'Client\' can\'t be instantiated');
        }

        this._tasks = [];
    }

    init(file, authHosts, files) {
        this._authHosts = authHosts;
        this._files = files;
        this._file = file;
    }

    async run() {
        const writer = new CSVFileWriter();
        const results = await new ExecutorService().execute('./run.sh', this._tasks);
        const columns = [
            'task_id',
            'execution_time'
        ];

        const computeAvg = (data) => {
            const avg = data.reduce((prevValue, currValue) => {
                return prevValue + currValue;
            }, 0) / data.length;


            return avg.toFixed(3);
        };

        const rows = [];
        const data = [];

        console.log(results.keys(), results.values());

        results.forEach((value, key) => {
            rows.push({
                task_id: key,
                execution_time: value
            });

            data.push(value);
        });

        writer.write(this._file, { rows, columns });

        return computeAvg(data);
    }
};

module.exports = Client;
