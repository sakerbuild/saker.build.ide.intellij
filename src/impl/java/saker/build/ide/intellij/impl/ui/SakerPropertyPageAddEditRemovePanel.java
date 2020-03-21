package saker.build.ide.intellij.impl.ui;

import com.intellij.ui.AddEditRemovePanel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class SakerPropertyPageAddEditRemovePanel<T> extends AddEditRemovePanel<T> {
    {
        getTable().setShowColumns(true);
        getTable().getTableHeader().setReorderingAllowed(false);
    }

    public SakerPropertyPageAddEditRemovePanel(TableModel<T> model) {
        this(model, new ArrayList<>());
    }

    public SakerPropertyPageAddEditRemovePanel(TableModel<T> model, List<T> data) {
        super(model, data);
    }

    public SakerPropertyPageAddEditRemovePanel(TableModel<T> model, List<T> data, @Nullable String label) {
        super(model, data, label);
    }

    @Override
    protected boolean removeItem(T o) {
        return true;
    }

}
