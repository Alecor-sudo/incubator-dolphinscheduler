/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.escheduler.dao.upgrade;

import cn.escheduler.common.enums.DbType;
import cn.escheduler.common.utils.SchemaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * upgrade manager
 */
public class DolphinSchedulerManager {
    private static final Logger logger = LoggerFactory.getLogger(DolphinSchedulerManager.class);
    UpgradeDao upgradeDao;

    private void initUpgradeDao() {
        DbType dbType = UpgradeDao.getDbType();
        if (dbType != null) {
            switch (dbType) {
                case MYSQL:
                    upgradeDao = MysqlUpgradeDao.getInstance();
                    break;
                case POSTGRESQL:
                    upgradeDao = PostgresqlUpgradeDao.getInstance();
                    break;
                default:
                    logger.error("not support sql type: {},can't upgrade", dbType);
                    throw new IllegalArgumentException("not support sql type,can't upgrade");
            }
        }
    }

    public DolphinSchedulerManager() {
        initUpgradeDao();
    }

    public void initDolphinScheduler() {
        // Determines whether the escheduler table structure has been init
        if(upgradeDao.isExistsTable("t_escheduler_version") || upgradeDao.isExistsTable("t_escheduler_queue")) {
            logger.info("The database has been initialized. Skip the initialization step");
            return;
        }
        this.initDolphinSchedulerSchema();
    }

    public void initDolphinSchedulerSchema() {

        logger.info("Start initializing the DolphinScheduler manager table structure");
        upgradeDao.initSchema();
    }


    /**
     * upgrade DolphinScheduler
     */
    public void upgradeDolphinScheduler() throws Exception{

        // Gets a list of all upgrades
        List<String> schemaList = SchemaUtils.getAllSchemaList();
        if(schemaList == null || schemaList.size() == 0) {
            logger.info("There is no schema to upgrade!");
        }else {

            String version = "";
            // Gets the version of the current system
            if (upgradeDao.isExistsTable("t_escheduler_version")) {
                version = upgradeDao.getCurrentVersion("t_escheduler_version");
            }else if(upgradeDao.isExistsTable("t_dolphinscheduler_version")){
                version = upgradeDao.getCurrentVersion("t_dolphinscheduler_version");
            }else if(upgradeDao.isExistsColumn("t_escheduler_queue","create_time")){
                version = "1.0.1";
            }else if(upgradeDao.isExistsTable("t_escheduler_queue")){
                version = "1.0.0";
            }else{
                logger.error("Unable to determine current software version, so cannot upgrade");
                throw new RuntimeException("Unable to determine current software version, so cannot upgrade");
            }
            // The target version of the upgrade
            String schemaVersion = "";
            for(String schemaDir : schemaList) {
                schemaVersion = schemaDir.split("_")[0];
                if(SchemaUtils.isAGreatVersion(schemaVersion , version)) {

                    logger.info("upgrade DolphinScheduler metadata version from " + version + " to " + schemaVersion);

                    logger.info("Begin upgrading DolphinScheduler's table structure");
                    upgradeDao.upgradeDolphinScheduler(schemaDir);
                    version = schemaVersion;
                }

            }
        }

        // Assign the value of the version field in the version table to the version of the product
        upgradeDao.updateVersion(SchemaUtils.getSoftVersion());
    }
}
