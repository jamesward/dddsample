package se.citerus.dddsample.tracking.core.domain.patterns.entity;

import static se.citerus.dddsample.tracking.core.domain.patterns.OrmUtils.unwrapOrmProxy;

/**
 * Supporting base class for entities.
 *
 * While the Entity interface makes the pattern properties explicit,
 * this class is less general and is suited for this particular application.
 * </p>
 * For example, the private _primaryKey field is meant for autogenerated, numerical surrogate primary keys.
 */
public abstract class EntitySupport<T extends Entity, ID> implements Entity<T, ID> {

  @SuppressWarnings("UnusedDeclaration")
  private final Long _primaryKey = null;
  
  @Override
  public final boolean sameAs(final T other) {
    return other != null && this.identity().equals(other.identity());
  }

  @Override
  public final int hashCode() {
    return identity().hashCode();
  }

  @SuppressWarnings({"SimplifiableIfStatement", "unchecked", "EqualsWhichDoesntCheckParameterClass"})
  @Override
  public final boolean equals(final Object other) {
    if (other == null) return false;
    if (this == other) return true;
    if (unwrapOrmProxy(this).getClass() != unwrapOrmProxy(other).getClass()) return false;

    return sameAs((T) other);
  }

}
