SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

DROP SCHEMA IF EXISTS `midPoint` ;
CREATE SCHEMA IF NOT EXISTS `midPoint` DEFAULT CHARACTER SET utf8 ;
USE `midPoint` ;

-- -----------------------------------------------------
-- Table `midPoint`.`Objects`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Objects` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Objects` (
  `uuid` VARCHAR(36) NOT NULL ,
  `version` INT NULL ,
  `repomod` DATETIME NOT NULL ,
  `dtype` VARCHAR(45) NOT NULL ,
  `objectType` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `midPoint`.`Accounts`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Accounts` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Accounts` (
  `uuid` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(250) NOT NULL ,
  `resource_uuid` VARCHAR(36) NOT NULL ,
  `user_uuid` VARCHAR(36) NULL ,
  `object_class` VARCHAR(256) NOT NULL ,
  PRIMARY KEY (`uuid`) ,
  CONSTRAINT `FK_Accounts`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE UNIQUE INDEX `name_UNIQUE` ON `midPoint`.`Accounts` (`name` ASC) ;

CREATE INDEX `FK_Accounts_Resources` ON `midPoint`.`Accounts` (`resource_uuid` ASC) ;

CREATE INDEX `FK_Accounts_Persons` ON `midPoint`.`Accounts` (`user_uuid` ASC) ;

CREATE INDEX `FK_Accounts` ON `midPoint`.`Accounts` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`BooleanProperties`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`BooleanProperties` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`BooleanProperties` (
  `uuid` VARCHAR(36) NOT NULL ,
  `attrname` VARCHAR(128) NOT NULL ,
  `value` BIT NULL ,
  PRIMARY KEY (`uuid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `midPoint`.`DateProperties`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`DateProperties` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`DateProperties` (
  `uuid` VARCHAR(36) NOT NULL ,
  `attrname` VARCHAR(128) NOT NULL ,
  `value` DATETIME NULL ,
  PRIMARY KEY (`uuid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `midPoint`.`Domains`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Domains` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Domains` (
  `uuid` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(128) NOT NULL ,
  `regex` VARCHAR(500) NOT NULL ,
  `reserved_regex` VARCHAR(500) NOT NULL ,
  `unallowable_regex` VARCHAR(500) NOT NULL ,
  `domain_uuid` VARCHAR(36) NULL ,
  PRIMARY KEY (`uuid`) ,
  CONSTRAINT `FK_Domains`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE UNIQUE INDEX `name_UNIQUE` ON `midPoint`.`Domains` (`name` ASC) ;

CREATE INDEX `FK_Domains` ON `midPoint`.`Domains` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`GenericEntities`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`GenericEntities` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`GenericEntities` (
  `uuid` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(128) NOT NULL ,
  PRIMARY KEY (`uuid`) ,
  CONSTRAINT `FK_GenericEntities`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `FK_GenericEntities` ON `midPoint`.`GenericEntities` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`IntegerProperties`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`IntegerProperties` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`IntegerProperties` (
  `uuid` VARCHAR(36) NOT NULL ,
  `attrname` VARCHAR(128) NOT NULL ,
  `value` INT NULL ,
  PRIMARY KEY (`uuid`) )
ENGINE = InnoDB
COMMENT = '\n';


-- -----------------------------------------------------
-- Table `midPoint`.`Users`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Users` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Users` (
  `uuid` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(128) NOT NULL ,
  `familyName` VARCHAR(128) NULL ,
  `fullName` VARCHAR(128) NULL ,
  `givenName` VARCHAR(128) NULL ,
  `employeeNumber` varchar(128) DEFAULT NULL,
  `honorificPrefix` varchar(128) DEFAULT NULL,
  `honorificSuffix` varchar(128) DEFAULT NULL,
  `locality` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uuid`) ,
  CONSTRAINT `FK_Users`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE UNIQUE INDEX `name_UNIQUE` ON `midPoint`.`Users` (`name` ASC) ;

CREATE INDEX `FK_Users` ON `midPoint`.`Users` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`Resources`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Resources` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Resources` (
  `uuid` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(128) NOT NULL ,
  `type` VARCHAR(128) NOT NULL ,
  `namespace` VARCHAR(128) NOT NULL ,
  PRIMARY KEY (`uuid`) ,
  CONSTRAINT `FK_Resources`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE UNIQUE INDEX `name_UNIQUE` ON `midPoint`.`Resources` (`name` ASC) ;

CREATE INDEX `FK_Resources` ON `midPoint`.`Resources` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`StringProperties`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`StringProperties` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`StringProperties` (
  `uuid` VARCHAR(36) NOT NULL ,
  `attrname` VARCHAR(128) NOT NULL ,
  `value` MEDIUMTEXT NULL ,
  PRIMARY KEY (`uuid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `midPoint`.`Domains_Objects`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Domains_Objects` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Domains_Objects` (
  `domain_uuid` VARCHAR(36) NOT NULL ,
  `object_uuid` VARCHAR(36) NOT NULL ,
  CONSTRAINT `FK_Object`
    FOREIGN KEY (`object_uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `FK_Domain`
    FOREIGN KEY (`domain_uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `PR_ID` ON `midPoint`.`Domains_Objects` (`domain_uuid` ASC, `object_uuid` ASC) ;

CREATE INDEX `FK_Object` ON `midPoint`.`Domains_Objects` (`object_uuid` ASC) ;

CREATE INDEX `FK_Domain` ON `midPoint`.`Domains_Objects` (`domain_uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`Objects_Properties`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Objects_Properties` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Objects_Properties` (
  `object_uuid` VARCHAR(36) NOT NULL ,
  `property_type` CHAR NOT NULL ,
  `property_uuid` VARCHAR(36) NOT NULL ,
  `property_index` INT NOT NULL ,
  PRIMARY KEY (`object_uuid`, `property_index`) ,
  CONSTRAINT `FK_Object_Property`
    FOREIGN KEY (`object_uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `FK_Object_Property` ON `midPoint`.`Objects_Properties` (`object_uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`AccountAttributes`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`AccountAttributes` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`AccountAttributes` (
  `uuid` VARCHAR(36) NOT NULL ,
  `attrname` VARCHAR(255) NOT NULL ,
  `attrvalue` VARCHAR(255) NOT NULL ,
  CONSTRAINT `FK_AccountAttributes`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Accounts` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `FK_AccountAttributes` ON `midPoint`.`AccountAttributes` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`ResourceObjectShadows`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`ResourceObjectShadows` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`ResourceObjectShadows` (
  `uuid` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(250) NOT NULL ,
  `resource_uuid` VARCHAR(36) NOT NULL ,
  `object_class` VARCHAR(256) NOT NULL ,
  PRIMARY KEY (`uuid`) ,
  CONSTRAINT `FK_ResourceObjectShadows`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE UNIQUE INDEX `name_UNIQUE` ON `midPoint`.`ResourceObjectShadows` (`name` ASC) ;

CREATE INDEX `FK_ResourceObjectShadows_Resources` ON `midPoint`.`ResourceObjectShadows` (`resource_uuid` ASC) ;

CREATE INDEX `FK_ResourceObjectShadows` ON `midPoint`.`ResourceObjectShadows` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`ResourceObjectAttributes`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`ResourceObjectAttributes` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`ResourceObjectAttributes` (
  `uuid` VARCHAR(36) NOT NULL ,
  `attrname` VARCHAR(255) NOT NULL ,
  `attrvalue` VARCHAR(255) NOT NULL ,
  CONSTRAINT `FK_ResourceObjectAttributes`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`ResourceObjectShadows` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `FK_ResourceObjectAttributes` ON `midPoint`.`ResourceObjectAttributes` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`ResourcesStates`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`ResourcesStates` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`ResourcesStates` (
  `uuid` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(128) NULL ,
  `state` TEXT(32000) NULL ,
  `resource_uuid` VARCHAR(36) NOT NULL ,
  PRIMARY KEY (`uuid`) ,
  CONSTRAINT `FK_Resources_States`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `FK_Resources` ON `midPoint`.`ResourcesStates` (`uuid` ASC) ;

CREATE INDEX `FK_Resources_States_Resources` ON `midPoint`.`ResourcesStates` (`resource_uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`UserTemplates`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`UserTemplates` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`UserTemplates` (
  `uuid` VARCHAR(36) NOT NULL ,
  `name` VARCHAR(128) NOT NULL ,
  `template` TEXT(32000) NOT NULL ,
  PRIMARY KEY (`uuid`) ,
  CONSTRAINT `FK_objects`
    FOREIGN KEY (`uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE INDEX `FK_objects` ON `midPoint`.`UserTemplates` (`uuid` ASC) ;


-- -----------------------------------------------------
-- Table `openidm`.`objects`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Objects` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Objects` (
  `uuid` VARCHAR(36) NOT NULL ,
  `version` INT NULL ,
  `repomod` DATETIME NOT NULL ,
  `dtype` VARCHAR(45) NOT NULL ,
  PRIMARY KEY (`uuid`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `midPoint`.`Objects_additionalNames`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Objects_additionalNames` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Objects_additionalNames` (
  `Objects_uuid` CHAR(36) NOT NULL ,
  `element` VARCHAR(255) NULL DEFAULT NULL ,
  `index_position` INT(11) NULL DEFAULT NULL ,
  CONSTRAINT `FK831D3A16B47C44B`
    FOREIGN KEY (`Objects_uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` ))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci;

CREATE INDEX `FK831D3A16B47C44B` ON `midPoint`.`Objects_additionalNames` (`Objects_uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`Objects_EMailAddress`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Objects_EMailAddress` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Objects_EMailAddress` (
  `Objects_uuid` CHAR(36) NOT NULL ,
  `element` VARCHAR(255) NULL DEFAULT NULL ,
  `index_position` INT(11) NULL DEFAULT NULL ,
  CONSTRAINT `FK7D414203B47C44B`
    FOREIGN KEY (`Objects_uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` ))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci;

CREATE INDEX `FK7D414203B47C44B` ON `midPoint`.`Objects_EMailAddress` (`Objects_uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`Objects_employeeType`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Objects_employeeType` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Objects_employeeType` (
  `Objects_uuid` CHAR(36) NOT NULL ,
  `element` VARCHAR(255) NULL DEFAULT NULL ,
  `index_position` INT(11) NULL DEFAULT NULL ,
  CONSTRAINT `FKF2A89C73B47C44B`
    FOREIGN KEY (`Objects_uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` ))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci;

CREATE INDEX `FKF2A89C73B47C44B` ON `midPoint`.`Objects_employeeType` (`Objects_uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`Objects_organizationalUnit`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Objects_organizationalUnit` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Objects_organizationalUnit` (
  `Objects_uuid` CHAR(36) NOT NULL ,
  `element` VARCHAR(255) NULL DEFAULT NULL ,
  `index_position` INT(11) NULL DEFAULT NULL ,
  CONSTRAINT `FKC356046DB47C44B`
    FOREIGN KEY (`Objects_uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` ))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci;

CREATE INDEX `FKC356046DB47C44B` ON `midPoint`.`Objects_organizationalUnit` (`Objects_uuid` ASC) ;


-- -----------------------------------------------------
-- Table `midPoint`.`Objects_telephoneNumber`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `midPoint`.`Objects_telephoneNumber` ;

CREATE  TABLE IF NOT EXISTS `midPoint`.`Objects_telephoneNumber` (
  `Objects_uuid` CHAR(36) NOT NULL ,
  `element` VARCHAR(255) NULL DEFAULT NULL ,
  `index_position` INT(11) NULL DEFAULT NULL ,
  CONSTRAINT `FKC04BB322B47C44B`
    FOREIGN KEY (`Objects_uuid` )
    REFERENCES `midPoint`.`Objects` (`uuid` ))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci;

CREATE INDEX `FKC04BB322B47C44B` ON `midPoint`.`Objects_telephoneNumber` (`Objects_uuid` ASC) ;


;
-- ------------------------------------------------------------
-- Create Administrator account with password 'secret'
-- ------------------------------------------------------------
INSERT INTO `midPoint`.`Objects` (uuid,version,repomod,dtype)
	VALUES ("e9a1a3b1-9457-468e-852f-419c272ed69b",0,"2011-02-01 12:48:20","User");
INSERT INTO `midPoint`.`Users` (uuid,name,familyName,fullName,givenName)
	VALUES ("e9a1a3b1-9457-468e-852f-419c272ed69b","Administrator","Administrator","midPoint Administrator","midPoint");
INSERT INTO `midPoint`.`StringProperties` (uuid,attrname,value) 
	VALUES ("63d5b0f2-9752-41c2-8e3a-66ebad5002a3","credentials",'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<credentials xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd">
    <password>
        <c:hash xmlns:ns3="http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd"
		xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b</c:hash>
    </password>
    <allowedIdmGuiAccess>true</allowedIdmGuiAccess>
</credentials>
');
INSERT INTO `midPoint`.`Objects_Properties` (object_uuid,property_type,property_uuid,property_index) 
	VALUES ("e9a1a3b1-9457-468e-852f-419c272ed69b","S","63d5b0f2-9752-41c2-8e3a-66ebad5002a3",0);


-- --------------------------------------------------------------
-- Create user MIDPOINT_PROXY, grant privileges for schema midPoint
-- --------------------------------------------------------------
DROP USER 'MIDPOINT_PROXY'@'localhost';
DROP USER 'MIDPOINT_PROXY'@'%';
CREATE USER 'MIDPOINT_PROXY'@'localhost' IDENTIFIED BY 'J3dnu_42_cIzmU_s_PL0ch0U_P0drazk0U';
GRANT ALL PRIVILEGES ON `midPoint`.* TO 'MIDPOINT_PROXY'@'localhost';
CREATE USER 'MIDPOINT_PROXY'@'%' IDENTIFIED BY 'J3dnu_42_cIzmU_s_PL0ch0U_P0drazk0U';
GRANT ALL PRIVILEGES ON `midPoint`.* TO 'MIDPOINT_PROXY'@'%';
SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
