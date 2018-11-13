ko.validation.init();
        
function User(data) {
    var self = this;

    this.id = data.id;
    this.userName = ko.observable(data.userName).extend({ required: true });
    this.userLogin = ko.observable(data.userLogin).extend({ required: true, localUniqueStringAttribute: { params: { localList: function() { return AtlasCopco.viewModel == null ? [] : AtlasCopco.viewModel.items(); }, attribute: 'userLogin', object: this }, message: "User login must be unique." } });
    this.roles = ko.observableArray(data.roles).extend({ required: true });
    // this.active = ko.observableArray(data.active);
    this.createdAt = ko.observable(data.createdAt);
    this.modifiedAt = ko.observable(data.modifiedAt);
    this.allowSaveReset = ko.observable(true);
    this.dirtyFlag = new ko.dirtyFlag(this, { filteredItems: ["selected", "displayRegion", "errors", "valid", "allowSaveReset", "providedInformationMatchesRole" ] });
    this.valid = ko.validation.group(this);
}

AtlasCopco.baseViewModel.copyDataToModel = function(item, data) {
    item.userName(data.userName);
    item.userLogin(data.userLogin);
    item.roles(data.roles);
    // item.active(data.active);
    item.createdAt(data.createdAt);
    item.modifiedAt(data.modifiedAt);
    item.dirtyFlag.reset();
}

ko.utils.extend(AtlasCopco.baseViewModel.config, {
    model: User,
    messages: {
        saving: "Saving...",
        deleting: "Deleting...",
        success: "Success",
        itemIsNotValid: "User is not valid.",
        itemCreatedSuccesfully: "User created successfully.",
        itemUpdatedSuccessfully: "User updated successfully.",
        confirmItemDeletion: "Are you sure that you want to delete this user?",
        unableToResetNotSaved: "User has not been saved to server, unable to reset.",
        deletionError: "An error occurred when deleting the user: ",
        deletionSuccessful: "User deleted successfully.",
        errorRetrievingData: "An error occurred when retrieving data from server: ",
        errorSaving: "An error occurred when saving the user: ",
        resetSuccessful: "User information has been reloaded from server."
    },
    requiresId: false
});

function UsersViewModel() {
    var self = this;

    self.userRoles = ko.observableArray(["LITE_USER", "USER", "ADMINISTRATOR", "UPLOADER"]);
    self.items = ko.observableArray($.map(rawUsers, function(element) {
        return new User(element);
    }));
    self.selectedItem = AtlasCopco.baseViewModel.selectedItem;
    self.selectedRigscanUser = ko.observable();
    var itemIds = _.chain(rawUsers).map(function (item) {
        return item.id
    }).value();

    self.rigscanUsers = _.filter(rigscanUsers, function (user) {
        return itemIds.indexOf(user.id) === -1;
    });
    self.rigscanUsersById = {};
    _.each(self.rigscanUsers, function(item) {
        self.rigscanUsersById[item.id] = item;
    });
    self.isItemSelected = AtlasCopco.baseViewModel.isItemSelected(self);
    self.selectItem = AtlasCopco.baseViewModel.selectItem(self);
    self.deleteItem = AtlasCopco.baseViewModel.deleteItem(self);
    self.isCreation = function(item) {
        return itemIds.indexOf(item.id) === -1;
    };
    self.registerCreation = function (item) {
        itemIds.push(item.id)
    }
    self.saveItem = AtlasCopco.baseViewModel.saveItem;
    self.resetItem = AtlasCopco.baseViewModel.resetItem;
    self.addItem = AtlasCopco.baseViewModel.addItem(self);
    self.titleFor = function(item) {
        return item != null && item.userName() != null ? "Details: " + item.userName() : "Details";
    };
    self.sortColumn = AtlasCopco.baseViewModel.sortColumn;
    self.sortDirection = AtlasCopco.baseViewModel.sortDirection;
    self.sortBy = AtlasCopco.baseViewModel.onSortColumnClicked(self);
    self.doSort = AtlasCopco.baseViewModel.doSort(self);
    self.textFilter = ko.observable();
    var doFilter = function(newValue) {
        var textFilter = self.textFilter();

        // filter the items so that only the ones with the correct product company
        // are shown
        var filtered = _.chain(rawUsers)
            .filter(function (item) { return _.isUndefined(textFilter) || textFilter === null || item.userName.toLowerCase().indexOf(textFilter.toLowerCase()) > -1; })
            .map(function(item) { return new User(item); })
            .value();

        // set the items
        self.items(filtered);

        self.doSort();
    };
    self.textFilter.subscribe(doFilter);
    self.addNewUser = function() {
        if (!usersDownloadSucceeded) {
            alert("Failed to download users from main RigScan server. Log out and log in again to add users.");
            return;
        }

        var dialog = $("#add-user").dialog({
            modal: true,
            draggable: false,
            buttons: {
                Add: function () {
                    var rigscanUser = self.rigscanUsersById[self.selectedRigscanUser()];

                    var newUser = new User({id: rigscanUser.id});
                    newUser.userLogin(rigscanUser.userLogin);
                    newUser.userName(rigscanUser.userName);

                    $("#add-user").dialog("close");

                    self.items.push(newUser);
                    self.selectItem(newUser)
                },
                Cancel: function() {
                    $("#add-user").dialog("close");
                }
            },
            title: "Add new user..."
        });
    }
}

ko.utils.extend($.blockUI.defaults.css, {
    border: 'none',
    padding: '15px',
    backgroundColor: '#000',
    '-webkit-border-radius': '10px',
    '-moz-border-radius': '10px',
    opacity: .5,
    color: '#fff'
});

AtlasCopco.apiEndpoints.apiPath = "/api/users";

ko.applyBindings(AtlasCopco.viewModel = new UsersViewModel());