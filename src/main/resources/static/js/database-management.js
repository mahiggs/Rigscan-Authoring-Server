var AtlasCopco = {
    defaultXHRErrorHandler: function (mainErrorMessage, item) {
        return function (jqXHR, textStatus, errorThrown) {
            if (jqXHR.status == 0
                || _.isUndefined(jqXHR.responseText)) {
                // the request was cancelled or in some other way
                // returned no text
                return;
            }

            var errorInformation = $.parseJSON(jqXHR.responseText);
            var errorMessage = errorInformation["message"];

            if ("fields" in errorInformation) {
                errorMessage += "\n\nValidation error(s) occurred:";
                _.each(errorInformation["fields"], function (value, key, list) {
                    errorMessage += "\n - "+value.field+": "+value.error;
                });
            }

            AtlasCopco.delayedUnblockUI();

            if (item != null) {
                item.allowSaveReset(true);
            }

            alert(mainErrorMessage + errorMessage);
        };
    },
    blockUITimer: null,
    debug: true,
    delayedBlockUI: function (message, delay) {
        this.blockUITimer = setTimeout(function () {
            $.blockUI({ message: '<h1 style="color: white; font-size: 2em">' + message + '</h1>' });
        }, delay);
    },
    delayedUnblockUI: function () {
        clearTimeout(this.blockUITimer);
        $.unblockUI();
    },
    baseViewModel: {
        addItem: function (self) {
            return function () {
                var newItem = new AtlasCopco.baseViewModel.config.model({});
                console.log(newItem.valid());
                self.items.push(newItem);
                self.selectItem(newItem);
            };
        },
        config: {
            /**
             * This is a reference to the view model for the items that are being managed.
             */
            model: null,
            /**
             * This is a list of messages necessary for the user.
             */
            messages: {
                saving: "Saving...",
                deleting: "Deleting...",
                success: "Success",
                itemIsNotValid: "Item is not valid.",
                itemCreatedSuccesfully: "Item created successfully.",
                itemUpdatedSuccessfully: "Item updated successfully.",
                confirmItemDeletion: "Are you sure that you want to delete this item?",
                unableToResetNotSaved: "Item has not been saved to server, unable to reset.",
                deletionError: "An error occurred when deleting the item: ",
                deletionSuccessful: "Item deleted successfully.",
                errorRetrievingData: "An error occurred when retrieving data from server: ",
                errorSaving: "An error occurred when saving the item: ",
                errorWhileSearching: "An error occurred while searching items: ",
                resetSuccessful: "Item information has been reloaded from server."
            },
            /**
             * True if the endpoint requires an ID. This should only be true for distributed
             * entities.
             */
            requiresId: false,
            /**
             * The minimum number of characters required in order to perform the search.
             */
            minimumSearchLength: 2,
        },
        copyDataToModel: function(item, data) {
            if (AtlasCopco.debug && console && console.error) {
                console.error("copyDataToModel not implemented:", item, data);
            }
        },
        deleteItem: function (self) {
            return function (item) {
                // clear out item selection
                if (self.isItemSelected(item)) {
                    self.selectItem(null);
                }

                // if it hasn't been saved to the server
                if (item.id == null) {
                    // then we just need to remove the item
                    self.items.remove(item);
                    return;
                }

                // make sure the user really wants to delete the item
                if (confirm(AtlasCopco.baseViewModel.config.messages.confirmItemDeletion)) {
                    var entityUrl = AtlasCopco.apiEndpoints.idPath(item.id);

                    AtlasCopco.delayedBlockUI(AtlasCopco.baseViewModel.config.messages.deleting, 250);

                    var headers = {};
                    headers[csrfHeaderName] = csrfToken;

                    $.ajax(entityUrl, {
                        type: "DELETE",
                        headers: headers,
                        error: AtlasCopco.defaultXHRErrorHandler(AtlasCopco.baseViewModel.config.messages.deletionError, item),
                        success: function (data, textStatus, jqXHR) {
                            AtlasCopco.delayedUnblockUI();

                            $.growlUI(AtlasCopco.baseViewModel.config.messages.success,
                                AtlasCopco.baseViewModel.config.messages.deletionSuccessful);

                            self.items.remove(item);
                            self.selectedItem(null);
                        }
                    });
                }
            }
        },
        resetItem: function (item) {
            if (AtlasCopco.baseViewModel.isCreation(item)) {
                alert(AtlasCopco.baseViewModel.config.messages.unableToResetNotSaved);
                return;
            }

            // make sure we aren't already saving/resetting the item
            if (!item.allowSaveReset()) {
                return;
            }

            // start the save/reset
            item.allowSaveReset(false);

            // load
            $.ajax(AtlasCopco.apiEndpoints.idPath(item.id), {
                headers: {"Accept": "application/json"},
                type: "GET",
                success: function (data) {
                    AtlasCopco.baseViewModel.copyDataToModel(item, data);

                    item.allowSaveReset(true);

                    $.growlUI(AtlasCopco.baseViewModel.config.messages.success,
                        AtlasCopco.baseViewModel.config.messages.resetSuccessful);
                },
                error: AtlasCopco.defaultXHRErrorHandler(AtlasCopco.baseViewModel.config.messages.errorRetrievingData, item)
            }, 'json');
        },
        isCreation: function (item) { return item.id == null; },
        registerCreation: function (item) {},
        saveItem: function (item) {
            // if we are already performing a save/reset operation, then we need to go
            // ahead and exit
            if (!item.allowSaveReset()) {
                return;
            }

            // set up the UI blocker to show up in 250ms if this task hasn't already completed
            AtlasCopco.delayedBlockUI(AtlasCopco.baseViewModel.config.messages.saving, 250);

            // mark this item as being in the process of being saved/reset
            item.allowSaveReset(false);

            // check to see if we have any errors
            if (item.valid().length > 0) {
                // if we do, then we need to inform the user that the item is not valid
                alert(AtlasCopco.baseViewModel.config.messages.itemIsNotValid);

                // cancel the UI block
                AtlasCopco.delayedUnblockUI();

                // set the allow save/reset to true again
                item.allowSaveReset(true);

                // and exit early
                return;
            }

            // update the modifiedAt and add createdAt if necessary
            var modificationTime = new Date().toISOString();
            if (item.createdAt() == null) {
                item.createdAt(modificationTime);
            }
            item.modifiedAt(modificationTime);

            // get the path for saving/updating
            var savePath = AtlasCopco.viewModel.isCreation(item) ? AtlasCopco.apiEndpoints.basePath() : AtlasCopco.apiEndpoints.idPath(item.id);

            var headers = {};
            headers[csrfHeaderName] = csrfToken;

            $.ajax(savePath, {
                type: AtlasCopco.viewModel.isCreation(item) ? "POST" : "PUT",
                headers: headers,
                contentType: "application/json",
                data: ko.toJSON(item),
                success: function (data, textStatus, jqXHR) {
                    // if this was a creation
                    if (AtlasCopco.viewModel.isCreation(item)) {
                        // get the location of the newly created entity from the response
                        var location = jqXHR.getResponseHeader("Location");

                        // and reload all the data from the server
                        $.ajax(location, {
                            type: "GET",
                            success: function (innerData) {
                                // copy over the new id
                                item.id = innerData.id;

                                // copy all the rest of the data
                                AtlasCopco.baseViewModel.copyDataToModel(item, innerData);

                                // unblock the UI
                                AtlasCopco.delayedUnblockUI();

                                // allow save/reset again
                                item.allowSaveReset(true);

                                // inform the user that the item was created successfully
                                $.growlUI(AtlasCopco.baseViewModel.config.messages.success,
                                    AtlasCopco.baseViewModel.config.messages.itemCreatedSuccesfully);
                            },
                            error: AtlasCopco.defaultXHRErrorHandler(AtlasCopco.baseViewModel.config.messages.errorRetrievingData, item)
                        }, 'json');
                    } else {
                        // remove the UI blocker
                        AtlasCopco.delayedUnblockUI();

                        // reset the dirty flag so that we don't show needing to save
                        item.dirtyFlag.reset();

                        // allow save/reset again
                        item.allowSaveReset(true);

                        // inform the user that the save was successful
                        $.growlUI(AtlasCopco.baseViewModel.config.messages.success,
                            AtlasCopco.baseViewModel.config.messages.itemUpdatedSuccessfully);
                    }
                },
                error: AtlasCopco.defaultXHRErrorHandler(AtlasCopco.baseViewModel.config.messages.errorSaving, item)
            });
        },
        selectedItem: ko.observable(),
        isItemSelected: function (self) {
            return function (item) {
                return item === self.selectedItem();
            };
        },
        selectItem: function (self) {
            return function (item) {
                // Clear out the previous selected item. This is important because if we have options
                // for a select depending upon the selected item, then it can cause data to be cleared
                // out.
                self.selectedItem(null);

                // select the item
                self.selectedItem(item);

                // force validation
                if (item != null && !_.isUndefined(item.valid)) {
                    item.valid.showAllMessages();
                }

                if (AtlasCopco.debug && item) {
                    console.log(ko.toJSON(ko.toJS(item)));
                }

                var dialogTitle = "Details";

                if (!_.isUndefined(self.titleFor)) {
                    dialogTitle = self.titleFor(item);
                }

                if (item == null) {
                    $("#details").dialog("close");
                    return;
                }

                var dialog = $("#details").dialog({
                    modal: true,
                    draggable: false,
                    buttons: {
                        Save: function () {
                            AtlasCopco.baseViewModel.saveItem(self.selectedItem());
                        },
                        Reset: function () {
                            AtlasCopco.baseViewModel.resetItem(self.selectedItem());
                        },
                        Delete: function() {
                            self.deleteItem(self.selectedItem());
                        }
                    },
                    title: dialogTitle
                });
            };
        },
        search: function(self) {
            return function (newValue) {
                if (self.searchRequest && self.searchRequest.abort) {
                    self.searchRequest.abort();
                }

                // remove all items that are actually on the server and
                // do not have any changes
                self.items.removeAll(_.filter(self.items(), function (item) {
                    return item.id != null && !item.dirtyFlag.isDirty();
                }));

                // unselect item manually
                if (!_.contains(self.items(), self.selectedItem())) {
                    self.selectedItem(null);
                }

                // if the search term is too short, then we don't search
                if (newValue.length <= AtlasCopco.baseViewModel.config.minimumSearchLength) {
                    return;
                }

                // indicate that we have started the search (this is used to show
                // the user that something is happening)
                if (typeof (self.isSearching) === "function") {
                    self.isSearching(true);
                }

                self.searchRequest = $.ajax(AtlasCopco.apiEndpoints.searchPath, {
                    type: "POST",
                    data: {
                        searchTerm: newValue
                    },
                    error: AtlasCopco.defaultXHRErrorHandler(AtlasCopco.baseViewModel.config.errorWhileSearching),
                    success: function (data) {
                        // inform the UI that we have finished searching
                        if (typeof (self.isSearching) === "function") {
                            self.isSearching(false);
                        }

                        self.searchRequest = null;

                        // Of the implementation has provided a way to handle the extra data
                        // then we call that method. Note that it is important that this be
                        // invoked before the addition of the main data as this data might be
                        // required in order to properly display the main data.
                        if (self.handleExtraData) {
                            self.handleExtraData(data.extraData);
                        }

                        // Add each of the main items.
                        _.each(data.searchResults, function (item) {
                            self.items.push(new AtlasCopco.baseViewModel.config.model(item));
                        });
                    }
                });
            };
        },
        sortColumn: ko.observable(),
        sortDirection: ko.observable(),
        onSortColumnClicked: function(self) {
            return function(model, event) {
                // retrieve the column that needs to be sorted
                var $target = $(event.target);
                var columnName = $target.data("sortColumn");

                // set the column and direction that is currently being sorted
                if (self.sortColumn() === columnName) {
                    self.sortDirection(self.sortDirection() === "ASC" ? "DESC" : "ASC");
                } else {
                    self.sortDirection("ASC");
                }
                self.sortColumn(columnName);

                // remove the old arrow
                $target.parent().find(".ui-icon").remove();

                // add the new arrow
                $target.append("<span></span>")
                    .find("span")
                    .addClass("ui-icon")
                    .addClass(self.sortDirection() === "ASC" ? "ui-icon-arrow-1-s" : "ui-icon-arrow-1-n")
                    .css("display", "inline-block");

                // perform the actual sorting
                self.doSort();
            };
        },
        doSort: function(self) {
            return function () {
                // get the sorting column
                var columnName = self.sortColumn();
                
                // no need to sort
                if (columnName == null) {
                    return;
                }

                // perform the sort
                self.items().sort(function (left, right) {
                    var leftValue = left[columnName](), rightValue = right[columnName]();

                    if (leftValue === rightValue) {
                        // comparison chain with ID so the sort is deterministic
                        return left.id < right.id ? -1 : 1;
                    } else {
                        var sortValue = left[columnName]() < right[columnName]() ? -1 : 1;

                        return self.sortDirection() === "ASC" ? sortValue : -sortValue;
                    }
                });

                // inform the UI that the list of items has been changed
                self.items.valueHasMutated();
            };
        },
        handleExtraData: function(data) {
            
        }
    },
    apiEndpoints: {
        apiPath: null,
        searchPath: null,
        basePath: function (id) {
            return AtlasCopco.apiEndpoints.apiPath;
        },
        idPath: function (id) {
            return AtlasCopco.apiEndpoints.apiPath + "/" + id;
        }
    }
};
