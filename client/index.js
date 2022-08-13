'use strict';

const Path = require('path');
const fs = require('fs');
const { CSVFileWriter } = require('./file_service');
const ListClient = require('./list_client');
const UploadClient = require('./upload_client');
const DownloadClient = require('./download_client');
const AppendClient = require('./append_client');

const TYPE = {
    LIST: 'list',
    UPLOAD: 'upload',
    DOWNLOAD: 'download',
    APPEND: 'append'
};

/**
 * convert it into a client class which will execute a complex task:
 * 
 * - list multiple directories (could be only one file)
 * - upload multiple files (could be only one file)
 * - download multiple files (could be only one file)
 * - append to multiple files (could be only one file)
 */
const parseArgs = () => {
    const args = process.argv.slice(2);

    if (args.length !== 4) {
        console.log(`Wrong arguments: node ${__filename} <type> <file_in> <file_out1> <file_out2>`);
        console.log('\type - client type: list, upload, download, append');
        console.log('\tfile_in - path to the JSON test input file');
        console.log('\file_out1 - path to the file where to store excution time for each task');
        console.log('\file_out2 - path to the file where to store average excution time of tasks');
        process.exit(-1);
    }

    return {
        type: args[0],
        file_in: args[1],
        file_out1: args[2],
        file_out2: args[3]
    }
};

const main = async () => {
    const {
        type,
        file_in,
        file_out1,
        file_out2
    } = parseArgs();

    const data = require(Path.join(__dirname, file_in));

    const filepath = Path.join(__dirname, file_out1);
    const authHosts = [];
    const files = [];

    data.forEach((input) => {
        authHosts.push(input.auth_host);
        files.push(input.files);
    });
    
    let client = null;
    
    switch (type) {
        case TYPE.LIST: client = new ListClient(); break;
        case TYPE.UPLOAD: client = new UploadClient(); break;
        case TYPE.DOWNLOAD: client = new DownloadClient(); break;
        case TYPE.APPEND: client = new AppendClient(); break;
        default: console.error('Unsupported task'); process.exit(1);
    }

    client.init(filepath, authHosts, files);
    console.log(`Number of tasks(clients): ${authHosts.length} is ready to start`);
    const avgTime = await client.run();
    console.log(`avgTime: ${avgTime}; Number of tasks(clients): ${authHosts.length}`);

    const writer = new CSVFileWriter();
    const avgfilepath = Path.join(__dirname, file_out2);
    const row = {
        nr_tasks: authHosts.length,
        avg_execution_time: avgTime
    };

    if (!fs.existsSync(avgfilepath)) {
        const columns = [
            'nr_tasks',
            'avg_execution_time'
        ];

        writer.write(avgfilepath, { columns, rows: [row] })
    } else {
        writer.write(avgfilepath, { columns: undefined, rows: [row] })
    }
};

main();
