'use strict';

const Client = require('./client');
const { TaskBuilder, TASK } = require('./builder');

class AppendClient extends Client {
    init(file, authHosts, files) {
        super.init(file, authHosts, files);

        const builder = new TaskBuilder();
        let id = 0;

        this._authHosts.forEach((authHost) => {
            this._tasks.push(builder.build(TASK.APPEND, {
                id: id + 1,
                user: authHost.user,
                password: authHost.password,
                host: authHost.host,
                port: authHost.port,
                files: files[id]
            }));
            ++id;
        });
    }
};

module.exports = AppendClient;
