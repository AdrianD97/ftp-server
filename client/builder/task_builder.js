'use strict';

const Builder = require('./builder');
const {
    OpenCommand,
    UserCommand,
    LsCommand,
    PutCommand,
    GetCommand,
    AppendCommand
} = require('../commands');

const {
    ListTask,
    UploadTask,
    DownloadTask,
    AppendTask
} = require('../tasks');

const TASK = {
    LIST: 'list',
    UPLOAD: 'upload',
    DOWNLOAD: 'download',
    APPEND: 'append'
};

class TaskBuilder extends Builder {
    build(name, args) {
        switch (name) {
            case TASK.LIST: return this._buildList(args);
            case TASK.UPLOAD: return this._buildUpload(args);
            case TASK.DOWNLOAD: return this._buildDownload(args);
            case TASK.APPEND: return this._buildAppend(args);
        }

        return null;
    }

    _buildList(args) {
        const {
            host,
            port,
            user,
            password,
            files,
            id
        } = args;

        const task = new ListTask(id);

        task.addCommand(new OpenCommand(host, port));
        task.addCommand(new UserCommand(user, password));
        files.forEach((file) => {
            task.addCommand(new LsCommand(file));
        });

        return task;
    }

    _buildUpload(args) {
        const {
            host,
            port,
            user,
            password,
            files,
            id
        } = args;

        const task = new UploadTask(id);

        task.addCommand(new OpenCommand(host, port));
        task.addCommand(new UserCommand(user, password));
        files.forEach(({ localfile, remotefile }) => {
            task.addCommand(new PutCommand(localfile, remotefile));
        });

        return task;
    }

    _buildDownload(args) {
        const {
            host,
            port,
            user,
            password,
            files,
            id
        } = args;

        const task = new DownloadTask(id);

        task.addCommand(new OpenCommand(host, port));
        task.addCommand(new UserCommand(user, password));
        files.forEach(({ remotefile, localfile }) => {
            task.addCommand(new GetCommand(remotefile, localfile));
        });

        return task;
    }

    _buildAppend(args) {
        const {
            host,
            port,
            user,
            password,
            files,
            id
        } = args;

        const task = new AppendTask(id);

        task.addCommand(new OpenCommand(host, port));
        task.addCommand(new UserCommand(user, password));
        files.forEach(({ localfile, remotefile }) => {
            task.addCommand(new AppendCommand(localfile, remotefile));
        });

        return task;
    }
};

module.exports = {
    TaskBuilder,
    TASK
};
