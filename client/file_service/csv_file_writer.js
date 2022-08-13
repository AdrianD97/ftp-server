'use strict';

const fs = require('fs');
const { stringify } = require('csv-stringify');
const FileWriter = require('./file_writer');

class CSVFileWriter extends FileWriter {
    write(filename, { columns, rows }) {
        const stream = fs.createWriteStream(filename, { flags: 'a' });
        
        const options = {
            header: columns ? true : false,
            columns: columns
        };
        const writer = stringify(options);

        writer.pipe(stream);

        rows.forEach((row) => {
            writer.write(row);
        });
    } 
};

module.exports = CSVFileWriter;