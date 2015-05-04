package com.vaadin.components.grid.table;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.query.client.Properties;
import com.google.gwt.query.client.js.JsUtils;
import com.vaadin.client.widgets.Grid.Column;
import com.vaadin.components.common.js.JS;
import com.vaadin.components.common.js.JS.Setter;
import com.vaadin.components.grid.GridComponent;
import com.vaadin.components.grid.config.JSCell;
import com.vaadin.components.grid.config.JSColumn;
import com.vaadin.components.grid.config.JSStaticCell;
import com.vaadin.components.grid.data.DataItemContainer;
import com.vaadin.shared.ui.grid.GridConstants;
import com.vaadin.shared.ui.grid.Range;

public final class GridColumn extends Column<Object, Object> {

    private final JSColumn jsColumn;
    private final GridComponent gridComponent;
    private final Map<Integer, Object> generatorCache = new HashMap<>();

    public static GridColumn addColumn(JSColumn jsColumn,
            GridComponent gridComponent) {
        GridColumn result = new GridColumn(jsColumn, gridComponent);
        gridComponent.getGrid().addColumn(result);
        result.bindProperties();
        return result;
    }

    private GridColumn(JSColumn jsColumn, GridComponent gridComponent) {
        this.jsColumn = jsColumn;
        this.gridComponent = gridComponent;
    }

    private JSStaticCell getDefaultHeaderCellReference() {
        GridStaticSection staticSection = gridComponent.getStaticSection();
        return staticSection.getHeaderCellByColumn(staticSection.getDefaultHeader(),
                this);
    }

    private void bindProperties() {
        JS.definePropertyAccessors(jsColumn, "headerContent", v -> {
            getDefaultHeaderCellReference().setContent(v);
            gridComponent.redraw();
        }, () -> getDefaultHeaderCellReference().getContent());

        bind("flex", v -> setExpandRatio(((Double) v).intValue()));
        bind("sortable", v -> setSortable((Boolean) v));
        bind("readOnly", v -> setEditable(!(boolean) v));
        bind("renderer", v -> setRenderer((cell, data) -> {
            JSCell jsCell = JSCell.create(cell, gridComponent.getContainer());
            JS.exec(v, jsCell);
        }));
        bind("minWidth",
                v -> setMinimumWidth(JS.isUndefinedOrNull(v) ? GridConstants.DEFAULT_MIN_WIDTH
                        : (double) v));
        bind("maxWidth",
                v -> setMaximumWidth(JS.isUndefinedOrNull(v) ? GridConstants.DEFAULT_MAX_WIDTH
                        : (double) v));
        bind("width",
                v -> setMaximumWidth(JS.isUndefinedOrNull(v) ? GridConstants.DEFAULT_COLUMN_WIDTH_PX
                        : (double) v));

        bind("valueGenerator", v -> gridComponent.getDataSource().refresh());
    }

    private void bind(String propertyName, final Setter setter) {
        JS.definePropertyAccessors(jsColumn, propertyName, v -> {
            setter.setValue(v);
            gridComponent.redraw();
        }, null);
    }

    public JSColumn getJsColumn() {
        return jsColumn;
    }

    @Override
    public Object getValue(Object dataItem) {
        if (dataItem instanceof DataItemContainer) {
            dataItem = ((DataItemContainer) dataItem).getDataItem();
        }
        Object result = null;
        if (jsColumn.getValueGenerator() != null) {
            int index = gridComponent.getDataSource().indexOf(dataItem);
            if (generatorCache.containsKey(index)) {
                result = generatorCache.get(index);
            } else {
                result = JS.exec(jsColumn.getValueGenerator(), dataItem);
                generatorCache.put(index, result);
            }
        } else {
            if (JS.isPrimitiveType(dataItem)) {
                if (getColumnIndex() == 0) {
                    result = dataItem;
                }
            } else {
                if (JsUtils.isArray((JavaScriptObject) dataItem)) {
                    result = ((JsArrayMixed) dataItem)
                            .getObject(getColumnIndex());
                } else {
                    result = ((Properties) dataItem).getObject(jsColumn
                            .getName());
                }
            }
        }
        return result;
    }

    private int getColumnIndex() {
        return gridComponent.getColumns().indexOf(jsColumn);
    }

    public void filterGeneratorCache(Range visibleRange, Range refreshRange) {
        if (jsColumn.getValueGenerator() != null) {
            for (Integer index : new HashSet<Integer>(generatorCache.keySet())) {
                if (refreshRange.contains(index)
                        || !visibleRange.contains(index)) {
                    generatorCache.remove(index);
                }
            }
        }
    }

}
