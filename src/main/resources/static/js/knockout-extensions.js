ko.oneTimeDirtyFlag = function (root) {
    var _initialized;

    //one-time dirty flag that gives up its dependencies on first change
    var result = ko.computed(function () {
        if (!_initialized) {
            //just for subscriptions
            ko.toJS(root);

            //next time return true and avoid ko.toJS
            _initialized = true;

            //on initialization this flag is not dirty
            return false;
        }

        //on subsequent changes, flag is now dirty
        return true;
    });

    return result;
};

ko.dirtyFlag = function(root, options) {
    var opts = {
        filteredItems: [],
        treatEmptyStringAsNull: [],
        isInitiallyDirty: false
    };

    ko.utils.extend(opts, options);

    var filteredJSON = function(object) {
        var obj = ko.toJS(object);

        _.each(opts['filteredItems'], function(val) {
            delete obj[val];
        });

        _.each(opts['treatEmptyStringAsNull'], function(val) {
            if (obj[val] === "") {
                obj[val] = null;
            }
        });

        return ko.toJSON(obj);
    };
    var result = function () { },
        _initialState = ko.observable(filteredJSON(root)),
        _isInitiallyDirty = ko.observable(opts['isInitiallyDirty']);

    result.isDirty = ko.computed(function () {
        return _isInitiallyDirty() || _initialState() !== filteredJSON(root);
    });

    result.reset = function () {
        _initialState(filteredJSON(root));
        _isInitiallyDirty(false);
    };

    return result;
};

ko.bindingHandlers.select2 = {
    init: function (element, valueAccessor) {
        var value = valueAccessor();

        $(element).select2(value);

        // subscribe to the event so it will properly handle resets
        ko.bindingHandlers.select2.doSubscription(element, value);

        ko.utils.domNodeDisposal.addDisposeCallback(element, function () {
            $(element).select2('destroy');
            element.atlas_copco_subscription = undefined;
        });
    },
    update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        // subscribe to the event so it will properly handle resets
        ko.bindingHandlers.select2.doSubscription(element, valueAccessor());

        // inform select2 that what is selected has changed
        $(element).trigger('change');
    },
    doSubscription: function (element, value) {
        // if the value is an array, we don't want to subscribe to it since
        // that causes a stack overflow
        if ((value.value && _.isArray(value.value()))
            || (value.numericValue && _.isArray(value.numericValue()))) {
            return;
        }

        // if there's already a subscription on this element, cancel it
        if (element.atlas_copco_subscription) {
            element.atlas_copco_subscription.dispose();
        }

        // subscribe to the value and fire a change
        if (value.value) {
            element.atlas_copco_subscription = value.value.subscribe(function(newValue) {
                $(element).trigger("change");
            });
        }

        // handle numericValue as well
        if (value.numericValue) {
            element.atlas_copco_subscription = value.numericValue.subscribe(function(newValue) {
                $(element).trigger("change");
            });
        }
    }
};

ko.bindingHandlers.numericValue = {
    init: function (element, valueAccessor, allBindingsAccessor) {
        var underlyingObservable = valueAccessor();
        var interceptor = ko.dependentObservable({
            read: underlyingObservable,
            write: function (value) {
                if (value === "") {
                    underlyingObservable(null);
                } else if (!isNaN(value)) {
                    underlyingObservable(parseInt(value));
                }
            }
        });
        ko.bindingHandlers.value.init(element, function () { return interceptor; }, allBindingsAccessor);
    },
    update: ko.bindingHandlers.value.update
}

ko.validation.rules['uniqueStringAttribute'] = {
    async: true,
    validator: function (val, parms, callback) {
        $.ajax(AtlasCopco.apiEndpoints.basePath(), {
            data: {
                "$filter": parms.attribute + " eq '" + val + "'"
            },
            success: function (data) {
                // if there are no users with this login or if the only
                // user in the results is the current user, then we are unique
                if (_.isArray(data) && (data.length === 0 || (data.length === 1
                    && data[0].id === parms.object.id))) {
                    callback(true);
                } else {
                    // otherwise we are not unique
                    callback(false);
                }
            },
            error: function () {
                callback(true);
            }
        }, 'json');
    },
    message: "This must be unique."
};

ko.validation.rules['localUniqueStringAttribute'] = {
    async: true,
    validator: function (val, params, callback) {
        var localList = params.localList();

        var matchingItems = _.chain(localList)
            .filter(function (item) {
                return item[params.attribute] === val;
            })
            .filter(function (item) {
                return item.id !== params.object.id;
            })
            .value();

        callback(matchingItems.length === 0);
    },
    message: "This must be unique."
};

ko.validation.rules['minElements'] = {
    validator: function(val, init) {
        return val.length >= init;
    },
    message: "You must select at least {0} element(s)."
}

ko.validation.registerExtenders();