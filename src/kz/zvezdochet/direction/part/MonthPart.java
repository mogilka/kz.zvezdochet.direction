package kz.zvezdochet.direction.part;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.decoration.InfoDecoration;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.provider.TransitLabelProvider;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.part.Messages;
import kz.zvezdochet.provider.PlaceProposalProvider;
import kz.zvezdochet.provider.PlaceProposalProvider.PlaceContentProposal;

/**
 * Сводная таблица транзитов за месяц
 * @author Natalie Didenko
 */
public class MonthPart extends ModelListView implements ICalculable {
	@Inject
	public MonthPart() {}

	private Event person;
	private Place trplace;

	private DateTime dt;
	private DateTime dt2;
	private Text txPlace;
	private Text txLatitude;
	private Text txLongitude;
	private Text txZone;
	private Text txGreenwich;

	public void setPerson(Event person) {
		this.person = person;
	}

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}

	@Override
	public boolean check(int mode) throws Exception {
//		if (null == dt.getSelection())
//			dt.setSelection(new Date());
//		if (null == dt2.getSelection())
//			dt2.setSelection(new Date(System.currentTimeMillis() + 84600));
		if (null == trplace)
			trplace = new Place().getDefault();
		if (txZone.getText().equals(""))
			txZone.setText("0.0");

//		if (!DateUtil.isDateRangeValid(dt.getSelection(), dt2.getSelection())) {
//			DialogUtil.alertWarning("Укажите правильный период");
//			return false;
//		}
		if (null == person) {
			DialogUtil.alertWarning("Событие не задано");
			return false;
		}
		return true;
	}

	@Override
	public void initFilter(Composite parent) {
		grFilter = new Group(parent, SWT.NONE);
		grFilter.setText("Период");

		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Начало");
		dt = new DateTime(grFilter, SWT.DROP_DOWN);
//		dt.setNullText(""); //$NON-NLS-1$

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Конец");
		dt2 = new DateTime(grFilter, SWT.DROP_DOWN);
//		dt2.setNullText(""); //$NON-NLS-1$

		Group secPlace = new Group(grFilter, SWT.NONE);
		secPlace.setText(Messages.getString("PersonView.Place")); //$NON-NLS-1$
		txPlace = new Text(secPlace, SWT.BORDER);
		new InfoDecoration(txPlace, SWT.TOP | SWT.LEFT);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Latitude")); //$NON-NLS-1$
		txLatitude = new Text(secPlace, SWT.BORDER);
		txLatitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Longitude")); //$NON-NLS-1$
		txLongitude = new Text(secPlace, SWT.BORDER);
		txLongitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Greenwith")); //$NON-NLS-1$
		txGreenwich = new Text(secPlace, SWT.BORDER);
		txGreenwich.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Zone")); //$NON-NLS-1$
		txZone = new Text(secPlace, SWT.BORDER);

		GridLayoutFactory.swtDefaults().numColumns(10).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(secPlace);
		GridDataFactory.fillDefaults().span(4, 1).grab(true, false).applyTo(secPlace);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
//			grab(true, false).applyTo(dt);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
//			grab(true, false).applyTo(dt2);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			span(4, 1).grab(true, false).applyTo(txPlace);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLatitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLongitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txZone);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txGreenwich);
	}

	/**
	 * Поиск персоны, для которой рассчитывается транзит
	 * @return персона
	 */
	public Event getPerson() {
		return person;
	}

	@Override
	public void reset() {
//		dt.setNullText("");
//		dt2.setNullText("");
		txPlace.setText(""); //$NON-NLS-1$
		txLatitude.setText(""); //$NON-NLS-1$
		txLongitude.setText(""); //$NON-NLS-1$
		txZone.setText(""); //$NON-NLS-1$
		txGreenwich.setText(""); //$NON-NLS-1$
	}

	public Date getInitialDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, dt.getDay());
		calendar.set(Calendar.MONTH, dt.getMonth());
		calendar.set(Calendar.YEAR, dt.getYear());
		return calendar.getTime();
	}

	public Date getFinalDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, dt2.getDay());
		calendar.set(Calendar.MONTH, dt2.getMonth());
		calendar.set(Calendar.YEAR, dt2.getYear());
		return calendar.getTime();
	}

	@Override
	public Model createModel() {
		return null;
	}

	@Override
	protected String[] initTableColumns() {
		return new String[] {
			"",
			"Транзитная планета",
			"Аспект",
			"Натальный объект",
			"Направление",
			"Величина аспекта",
			"Знак Зодиака",
			"Дом",
			"Описание"
		};
	}

	/**
	 * Инициализация представления местности события
	 * @param place местность
	 */
	private void initPlace(Place place) {
		if (null == place) return;
		txPlace.setText(place.getName());
		txLatitude.setText(CalcUtil.formatNumber("###.##", place.getLatitude())); //$NON-NLS-1$
		txLongitude.setText(CalcUtil.formatNumber("###.##", place.getLongitude())); //$NON-NLS-1$
		txGreenwich.setText(CalcUtil.formatNumber("###.##", place.getGreenwich())); //$NON-NLS-1$
		txZone.setText(String.valueOf(place.getGreenwich()));
	}

	/**
	 * Инициализация местностей
	 */
	private void setPlaces() {
	    PlaceProposalProvider proposalProvider = new PlaceProposalProvider();
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txPlace, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Place place = (Place)((PlaceContentProposal)proposal).getObject();
				if (place != null) {
					trplace = place;
					initPlace(place);
				}
			}
		});
	}

	@Override
	protected void initControls() throws DataAccessException {
		super.initControls();
		setPlaces();
	}

	public Place getPlace() {
		return trplace;
	}

	public double getZone() {
		return Double.parseDouble(txZone.getText());
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new TransitLabelProvider();
	}

	@Override
	public void onCalc(Object obj) {
		// TODO Auto-generated method stub
		
	}
}
