# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### Unreleased

- Nothing yet

### [2.0.1] - 2023-11-16

#### Fixed

- Added a (for now) hardcoded waiting time before creating a submission to reduce chances of HTTP 403 issues

### [2.0.0] - 2023-11-14

#### Changed

- (BREAKING CHANGE) Method `createSubmission` now takes a `Map<String, String>` containing cookie name-value pairs.

### [1.1.0] - 2023-09-11

#### Added

- Added flag to turn on / off verification of supplied number of form step data elements against number of steps in form
- Added explicit deletion of submission steps 

#### Fixed

- Fixed logging, all calls will now be logged (request and response) in case of verification failure 

### [1.0.0] - 2023-09-04

#### Added

- First public, non-alpha/-beta version of the OpenForms TestClient 
