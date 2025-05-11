package kr.hhplus.be.server.application.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "LOCK:";

    private final LockService lockService;
    private final SpelExpressionParser parser;
    private final ParameterNameDiscoverer nameDiscoverer;

    @Around("@annotation(kr.hhplus.be.server.application.redis.DistributedLock)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        DistributedLock lockAnn = sig.getMethod()
                .getAnnotation(DistributedLock.class);

        String[] names = nameDiscoverer.getParameterNames(sig.getMethod());
        Object[] args = pjp.getArgs();
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < names.length; i++) {
            ctx.setVariable(names[i], args[i]);
        }

        String rawKey = lockAnn.key();
        String evaluated = parser
                .parseExpression(rawKey, new TemplateParserContext("{","}"))
                .getValue(ctx, String.class);
        String key = LOCK_PREFIX + evaluated;
        log.info("[DistributedLock] trying key={}", key);

        return lockService.executeWithLock(
                key,
                lockAnn.waitTime(),
                lockAnn.leaseTime(),
                lockAnn.timeUnit(),
                () -> {
                    try {
                        return pjp.proceed();
                    } catch (RuntimeException re) {
                        throw re;
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
        );
    }
}