package se.citerus.dddsample.domain.service.impl;

import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.domain.model.handling.HandlingEventRepository;
import se.citerus.dddsample.domain.service.DomainEventNotifier;
import se.citerus.dddsample.domain.service.HandlingEventService;

public final class HandlingEventServiceImpl implements HandlingEventService {

  private final HandlingEventRepository handlingEventRepository;
  private final DomainEventNotifier domainEventNotifier;

  public HandlingEventServiceImpl(HandlingEventRepository handlingEventRepository, DomainEventNotifier domainEventNotifier) {
    this.handlingEventRepository = handlingEventRepository;
    this.domainEventNotifier = domainEventNotifier;
  }

  public void register(final HandlingEvent event) {
    /*
     NOTE:
       The cargo instance that's loaded and associated with the handling event is
       in an inconsitent state, because the cargo delivery history's collection of
       events does not contain the event created here. However, this is not a problem,
       because cargo is in a different aggregate from handling event.

       The rules of an aggregate dictate that all consistency rules within the aggregate
       are enforced synchronously in the transaction, but consistency rules of other aggregates
       are enforced by asynchronous updates, after the commit of this transaction.
    */
    handlingEventRepository.save(event);

    domainEventNotifier.cargoWasHandled(event);
  }

}
