'use strict';

const Client = require('./client');
const { TaskBuilder, TASK } = require('./builder');

class ListClient extends Client {
    init(file, authHosts, files) {
        super.init(file, authHosts, files);

        const builder = new TaskBuilder();
        let id = 0;

        this._authHosts.forEach((authHost) => {
            this._tasks.push(builder.build(TASK.LIST, {
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

module.exports = ListClient;
