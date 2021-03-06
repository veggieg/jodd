// Copyright (c) 2003-2014, Jodd Team (jodd.org). All Rights Reserved.

package jodd.madvoc;

import jodd.madvoc.component.MadvocController;
import jodd.madvoc.filter.ActionFilter;
import jodd.madvoc.filter.BaseActionFilter;
import jodd.madvoc.interceptor.ActionInterceptor;
import jodd.madvoc.interceptor.BaseActionInterceptor;
import jodd.util.ReflectUtil;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;

public class ActionRequestRecursionTest {

	@Test
	public void testFiltersPassAndInterceptorsPass() throws Exception {
		MyActionRequest actionRequest = createMyActionRequest(
				arr(new FilterPass(1), new FilterPass(2)),
				arr(new InterceptorPass(1), new InterceptorPass(2))
		);

		actionRequest.invoke();
		assertEquals("-F1-F2-I1-I2-A-i2-i1-R-f2-f1", actionRequest.data);
	}

	@Test
	public void testFiltersStopAndInterceptorsPass1() throws Exception {
		MyActionRequest actionRequest = createMyActionRequest(
				arr(new FilterStop(), new FilterPass(2)),
				arr(new InterceptorPass(1), new InterceptorPass(2))
		);

		actionRequest.invoke();
		assertEquals("-X", actionRequest.data);
	}

	@Test
	public void testFiltersStopAndInterceptorsPass2() throws Exception {
		MyActionRequest actionRequest = createMyActionRequest(
				arr(new FilterPass(1), new FilterStop()),
				arr(new InterceptorPass(1), new InterceptorPass(2))
		);

		actionRequest.invoke();
		assertEquals("-F1-X-f1", actionRequest.data);
	}

	@Test
	public void testFiltersPassAndInterceptorsStop1() throws Exception {
		MyActionRequest actionRequest = createMyActionRequest(
				arr(new FilterPass(1), new FilterPass(2)),
				arr(new InterceptorPass(1), new InterceptorStop())
		);

		actionRequest.invoke();
		assertEquals("-F1-F2-I1-x-i1-R-f2-f1", actionRequest.data);
	}

	@Test
	public void testFiltersPassAndInterceptorsStop2() throws Exception {
		MyActionRequest actionRequest = createMyActionRequest(
				arr(new FilterPass(1), new FilterPass(2)),
				arr(new InterceptorStop(), new InterceptorPass(2))
		);

		actionRequest.invoke();
		assertEquals("-F1-F2-x-R-f2-f1", actionRequest.data);
	}

	@Test
	public void testFiltersPassAndInterceptorsStop3() throws Exception {
		MyActionRequest actionRequest = createMyActionRequest(
				arr(new FilterPass(1), new FilterPass(2)),
				arr(new InterceptorPass(1), new InterceptorStop(), new InterceptorPass(3))
		);

		actionRequest.invoke();
		assertEquals("-F1-F2-I1-x-i1-R-f2-f1", actionRequest.data);
	}

	// ---------------------------------------------------------------- internal

	public class MyActionRequest extends ActionRequest {
		public String data = "";
		public MyActionRequest(MadvocController madvocController, String actionPath, ActionConfig config, Object action, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
			super(madvocController, actionPath, config, action, servletRequest, servletResponse);
		}
		@Override
		protected Object invokeActionMethod() throws Exception {
			data += "-A";
			return super.invokeActionMethod();
		}
	}

	public class Action {
		public void view() {}
	}

	public class FilterPass extends BaseActionFilter {
		final int i;
		public FilterPass(int i) {
			this.i = i;
		}

		public Object filter(ActionRequest actionRequest) throws Exception {
			((MyActionRequest)actionRequest).data += "-F" + i;
			Object result = actionRequest.invoke();
			((MyActionRequest)actionRequest).data += "-f" + i;
			return result;
		}
	}
	public class FilterStop extends BaseActionFilter {
		public Object filter(ActionRequest actionRequest) throws Exception {
			((MyActionRequest)actionRequest).data += "-X";
			return "stop";
		}
	}

	public class InterceptorPass extends BaseActionInterceptor {
		final int i;
		public InterceptorPass(int i) {
			this.i = i;
		}

		public Object intercept(ActionRequest actionRequest) throws Exception {
			((MyActionRequest)actionRequest).data += "-I"+i;
			Object result = actionRequest.invoke();
			((MyActionRequest)actionRequest).data += "-i"+i;
			return result;
		}
	}
	public class InterceptorStop extends BaseActionInterceptor {
		public Object intercept(ActionRequest actionRequest) throws Exception {
			((MyActionRequest)actionRequest).data += "-x";
			return "stop";
		}
	}

	public class SimpleMadvocController extends MadvocController {
		@Override
		public void render(ActionRequest actionRequest, Object resultObject) throws Exception {
			((MyActionRequest)actionRequest).data += "-R";
		}
	}

	private MyActionRequest createMyActionRequest(ActionFilter[] actionFilters, ActionInterceptor[] actionInterceptors) {
		SimpleMadvocController madvocController = new SimpleMadvocController();

		Action action = new Action();
		ActionConfig actionConfig = new ActionConfig(
				Action.class,
				ReflectUtil.findMethod(Action.class, "view"),
				actionFilters, actionInterceptors,
				"path", "method", null, null, null);

		return new MyActionRequest(
				madvocController, "actionPath", actionConfig, action, null, null);
	}

	private <T> T[] arr(T... array) {
		return array;
	}

}