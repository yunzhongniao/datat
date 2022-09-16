-- datat.job_content definition

CREATE TABLE `job_content` (
  `job_content_id` bigint NOT NULL AUTO_INCREMENT,
  `job_name` varchar(100) DEFAULT NULL,
  `job_content` json DEFAULT NULL,
  PRIMARY KEY (`job_content_id`),
  UNIQUE KEY `JobId_un` (`job_content_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- datat.job_instance definition

CREATE TABLE `job_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_content_id` bigint NOT NULL,
  `start_timestamp` bigint DEFAULT NULL,
  `end_timestamp` bigint DEFAULT NULL,
  `state` varchar(100) DEFAULT NULL,
  `server_id` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- datat.tg_communication definition

CREATE TABLE `tg_communication` (
  `job_id` bigint NOT NULL,
  `tg_id` int NOT NULL,
  `tg_state` varchar(100) DEFAULT NULL,
  `tg_message` json DEFAULT NULL,
  `timestamp` bigint DEFAULT NULL,
  `tg_counter` json DEFAULT NULL,
  PRIMARY KEY (`job_id`,`tg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;